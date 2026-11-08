package uk.co.bithatch.macrolib;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Type;
import uk.co.bithatch.linuxio.InputController.Callback;
import uk.co.bithatch.linuxio.InputDevice;
import uk.co.bithatch.linuxio.InputDevice.Event;


/**
 * The Class ForwardDeviceHandler.
 */
public class ForwardDeviceHandler extends DeviceHandler implements Callback {

	final static Logger LOG = System.getLogger(ForwardDeviceHandler.class.getName());

	private boolean alt;
	private boolean ctrl;
	private int currentX = UInput.JOYSTICK_CENTER;
	private int currentY = UInput.JOYSTICK_CENTER;
	private List<String> digitalDown = new ArrayList<>();
	private List<EventCode> heldKeys = new ArrayList<>();
	private int lastX = UInput.JOYSTICK_CENTER;
	private int lastY = UInput.JOYSTICK_CENTER;
	private ScheduledFuture<?> moveTimer;
	private int moveX;
	private int moveY;
	private ScheduledExecutorService queue;
	private boolean shift;
	private UInput uinput;

	/**
	 * Instantiates a new forward device handler.
	 *
	 * @param uinput the uinput
	 * @param device the device
	 * @param callback the callback
	 * @param queue the queue
	 */
	public ForwardDeviceHandler(UInput uinput, MacroDevice device, EventConsumer callback,
			ScheduledExecutorService queue) {
		super(device, callback);
		this.uinput = uinput;
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		if(moveTimer != null) {
			moveTimer.cancel(false);
		}
	}

	/**
	 * Event.
	 *
	 * @param uinputDevice the uinput device
	 * @param event the event
	 */
	@Override
	public void event(InputDevice uinputDevice, Event event) {
		try {
			if(event.getCode().type() == EventCode.Type.EV_MSC) {
				/* Not currently interested in these at all */
				return;
			}
			
			if(LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("UInput event: %s (%s)", event, event.getCode().type()));
			
			if (event.getCode().type() == Type.EV_ABS) {
				if (device.getJoystickMode() == TargetType.JOYSTICK) {
					/*
					 * The kernel modules give a joystick position values between 0 and 255. The
					 * center is at 128. The virtual joysticks are set to give values between -127
					 * and 127. The center is at 0. So we adapt the received values.
					 */
					int val = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;
					uinput.emit(device.getJoystickMode(), event.getCode(), val, false);
				} else {
					updateJoystick(event);
				}
			} else if (event.getCode().type() == Type.EV_KEY) {
				KeyState state = event.getValue() == 1 ? KeyState.DOWN : KeyState.UP;
				if (event.getCode() == EventCode.BTN_X || event.getCode() == EventCode.BTN_Y
						|| event.getCode() == EventCode.BTN_Z) {
					if (device.getJoystickMode() == TargetType.MOUSE) {
						uinput.emit(TargetType.MOUSE, translateMouseButtons(event.getCode()), event.getValue(), true);
					} else if (device.getJoystickMode() == TargetType.DIGITAL_JOYSTICK) {
						uinput.emit(TargetType.DIGITAL_JOYSTICK, event.getCode(), event.getValue(), true);
					} else if (device.getJoystickMode() == TargetType.JOYSTICK) {
						uinput.emit(TargetType.JOYSTICK, event.getCode(), event.getValue(), true);
					} else {
						if (event.getValue() != 2) {
							callback.keyReceived(event.getCode(), state, event);
						}
					}
				} else {
					if (event.getValue() != 2) {
						callback.keyReceived(event.getCode(), state, event);
					}
				}
			} else if (event.getCode().type() == Type.EV_SYN) {
				if (device.getJoystickMode() == TargetType.JOYSTICK) {
					/* Just pass-through when in analogue joystick mode */
					uinput.emit(TargetType.JOYSTICK, event.getCode(), event.getValue(), false);
				}
			} else {
				callback.keyReceived(event.getCode(), event.getValue() == 1 ? KeyState.DOWN : KeyState.UP, event);
			}
		} catch (Exception ioe) {
			LOG.log(Level.ERROR, String.format("Failed to handle event: %s", event), ioe);
		}
	}

	/*
	 * Private
	 */

	int clamp(int minimum, int x, int maximum) {
		return Math.max(minimum, Math.min(x, maximum));
	}

	/**
	 * Calculate the distances from the (rough) centre position to the position when
	 * movement each axis will start emiting events based on the current calibration
	 * value.
	 * 
	 **/
	int[] computeBounds() {
		return new int[] { UInput.JOYSTICK_CENTER - (device.getJoystickCalibration()),
				UInput.JOYSTICK_CENTER + (device.getJoystickCalibration()) };
	}

	void digitalJoystick(Event event) throws IOException {

		int[] vals = computeBounds();
		int low_val = vals[0];
		int high_val = vals[1];
		int val = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;

		if (event.getCode() == EventCode.ABS_X) {
			if (val < low_val && !digitalDown.contains("l")) {
				digitalDown.add("l");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_X, UInput.JOYSTICK_MIN);
			} else if (val > high_val && !digitalDown.contains("r")) {
				digitalDown.add("r");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_X, UInput.JOYSTICK_MAX);
			} else if (val >= low_val && val <= high_val && digitalDown.contains("l")) {
				digitalDown.remove("l");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_X, UInput.JOYSTICK_CENTER);
			} else if (val >= low_val && val <= high_val && digitalDown.contains("r")) {
				digitalDown.remove("r");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_X, UInput.JOYSTICK_CENTER);
			}
		}

		if (event.getCode() == EventCode.ABS_Y) {
			if (val < low_val && !digitalDown.contains("u")) {
				digitalDown.add("u");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_Y, UInput.JOYSTICK_MIN);
			} else if (val > high_val && !digitalDown.contains("d")) {
				digitalDown.add("d");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_Y, UInput.JOYSTICK_MAX);
			} else if (val >= low_val && val <= high_val && digitalDown.contains("u")) {
				digitalDown.remove("u");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_Y, UInput.JOYSTICK_CENTER);
			} else if (val >= low_val && val <= high_val && digitalDown.contains("d")) {
				digitalDown.remove("d");
				uinput.emit(TargetType.DIGITAL_JOYSTICK, EventCode.ABS_X, UInput.JOYSTICK_CENTER);
			}
		}
	}

	/**
	 * Emit macro keys for joystick positions, so they can be processed as all other
	 * macro keys are (i.e. assigned to a macro, script, or a different uinput key)
	 * 
	 * @param event event
	 */
	void emitMacro(Event event) {
		int[] vals = computeBounds();
		int low_val = vals[0];
		int high_val = vals[1];
		int val = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;

		if (event.getCode() == EventCode.ABS_X) {
			if (val < low_val) {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_RIGHT), event);
				if (heldKeys.contains(EventCode.BTN_DPAD_LEFT)) {
					callback.keyReceived(EventCode.BTN_DPAD_LEFT, KeyState.DOWN, event);
					heldKeys.add(EventCode.BTN_DPAD_LEFT);
				}
			} else if (val > high_val) {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_LEFT), event);
				if (heldKeys.contains(EventCode.BTN_DPAD_RIGHT)) {
					callback.keyReceived(EventCode.BTN_DPAD_RIGHT, KeyState.DOWN, event);
					heldKeys.add(EventCode.BTN_DPAD_RIGHT);
				}
			} else {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_LEFT, EventCode.BTN_DPAD_RIGHT), event);
			}
		}

		if (event.getCode() == EventCode.ABS_Y) {
			if (val < low_val) {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_DOWN), event);
				if (heldKeys.contains(EventCode.BTN_DPAD_UP)) {
					callback.keyReceived(EventCode.BTN_DPAD_UP, KeyState.DOWN, event);
					heldKeys.add(EventCode.BTN_DPAD_UP);
				}
			} else if (val > high_val) {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_UP), event);
				if (heldKeys.contains(EventCode.BTN_DPAD_DOWN)) {
					callback.keyReceived(EventCode.BTN_DPAD_DOWN, KeyState.DOWN, event);
					heldKeys.add(EventCode.BTN_DPAD_DOWN);
				}
			} else {
				releaseKeys(Arrays.asList(EventCode.BTN_DPAD_UP, EventCode.BTN_DPAD_DOWN), event);
			}
		}
	}

	int modcode() {
		int code = 0;
		if (shift)
			code += 1;
		if (ctrl)
			code += 2;
		if (alt)
			code += 4;
		return code;
	}

	void mouseMove() {
		if (moveX != 0 || moveY != 0) {
			try {
				if (moveX != 0) {
					uinput.emit(TargetType.MOUSE, EventCode.REL_X, moveX);
				}
				if (moveY != 0) {
					uinput.emit(TargetType.MOUSE, EventCode.REL_Y, moveY);
				}
			} catch (IOException ioe) {
				throw new IllegalStateException("Failed move movement.", ioe);
			}
			moveTimer = queue.schedule(() -> mouseMove(), 100, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Update the current_x and current_y positions if this is an absolute movement
	 * event
	 */
	void recordCurrentAbsolutePosition(Event event) {
		if (event.getCode() == EventCode.ABS_X) {
			currentX = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;
		}
		if (event.getCode() == EventCode.ABS_Y) {
			currentY = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;
		}
	}

	void releaseKeys(List<EventCode> keys, Event event) {
		for (EventCode k : keys) {
			if (heldKeys.contains(k)) {
				callback.keyReceived(k, KeyState.UP, event);
				heldKeys.remove(k);
			}
		}
	}

	/**
	 * Translate the default joystick event codes to default mouse event codes
	 * 
	 * @param ecode event code to translate
	 */
	EventCode translateMouseButtons(EventCode ecode) {
		if (ecode == EventCode.BTN_X)
			return EventCode.BTN_LEFT;
		else if (ecode == EventCode.BTN_Y)
			return EventCode.BTN_RIGHT;
		else if (ecode == EventCode.BTN_Z)
			return EventCode.BTN_MIDDLE;
		else
			return ecode;
	}

	/**
	 * Handle a position update event from the joystick, either by translating it to
	 * mouse movements, digitising it, or emiting macros
	 * 
	 * @param event event
	 * @throws IOException
	 */
	void updateJoystick(Event event) throws IOException {
		if (device.getJoystickMode() == TargetType.DIGITAL_JOYSTICK) {
			recordCurrentAbsolutePosition(event);
			digitalJoystick(event);
		} else if (device.getJoystickMode() == TargetType.MOUSE) {
			int low_val = UInput.JOYSTICK_CENTER - device.getJoystickCalibration();
			int high_val = UInput.JOYSTICK_CENTER + device.getJoystickCalibration();

			if (event.getCode() == EventCode.REL_X) {
				currentX = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;
			}
			if (event.getCode() == EventCode.REL_Y) {
				currentY = event.getValue() - UInput.DEVICE_JOYSTICK_CENTER;
			}

			// Get the amount between the current value and the centre to move
			if (currentX >= high_val) {
				moveX = currentX - high_val;
			} else if (currentX <= low_val) {
				moveX = currentX - low_val;
			}

			if (currentY >= high_val) {
				moveY = currentY - high_val;
			} else if (currentY <= low_val) {
				moveY = currentY - low_val;
			}

			if (currentX != lastX || currentY != lastY) {
				lastX = currentX;
				lastY = currentY;
				moveX = clamp(-3, moveX / 8, 3);
				moveY = clamp(-3, moveY / 8, 3);
				mouseMove();
			} else {
				if (moveTimer != null) {
					moveTimer.cancel(false);
				}
			}
		} else {
			emitMacro(event);
		}
	}

}
