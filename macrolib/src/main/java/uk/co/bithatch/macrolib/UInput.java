package uk.co.bithatch.macrolib;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;
import uk.co.bithatch.linuxio.InputDevice.Event;

/**
 * The Class UInput.
 */
public class UInput implements Closeable {

	/** The Constant DEVICE_JOYSTICK_CENTER. */
	/*
	 * Value sent by the hardware when the joystick is at the center
	 */
	public final static int DEVICE_JOYSTICK_CENTER = 128;

	/** The Constant JOYSTICK_CENTER. */
	public final static int JOYSTICK_CENTER = 0;

	/** The Constant JOYSTICK_MAX. */
	public final static int JOYSTICK_MAX = 127;

	/** The Constant JOYSTICK_MIN. */
	/*
	 * Joystick calibration values
	 */
	public final static int JOYSTICK_MIN = -127;

	/** The Constant JS. */
	public final static int JS = 0x9999;

	/** The Constant JS_DOWN. */
	public final static int JS_DOWN = 0x9703;

	/** The Constant JS_LEFT. */
	public final static int JS_LEFT = 0x9701;

	/** The Constant JS_RIGHT. */
	public final static int JS_RIGHT = 0x9702;

	/** The Constant JS_UP. */
	public final static int JS_UP = 0x9704;

	/** The Constant MACROLIB_DIGITAL_JOYSTICK_PRODUCT_ID. */
	public final static int MACROLIB_DIGITAL_JOYSTICK_PRODUCT_ID = 0x0004;

	/*
	 * Special virtual keys that are actually joystick movement. These 'virtual'
	 * uinput codes are created so that the user can assign macros to the left,
	 * right, up and down directions of the joystick. By default uinput only has two
	 * codes (ABS_X and ABS_Y) that specify the axis, the direction being
	 * determinated by the value passed to uinput.emit.
	 */

	/** The Constant MACROLIB_JOYSTICK_PRODUCT_ID. */
	public final static int MACROLIB_JOYSTICK_PRODUCT_ID = 0x0002;

	/** The Constant MACROLIB_KEYBOARD_PRODUCT_ID. */
	public final static int MACROLIB_KEYBOARD_PRODUCT_ID = 0x0003;

	/** The Constant MACROLIB_MOUSE_PRODUCT_ID. */
	public final static int MACROLIB_MOUSE_PRODUCT_ID = 0x0001;

	/** The Constant MACROLIB_USB_VENDOR_ID. */
	/*
	 * These are the very unofficial vendor / produce codes used for the virtual
	 * devices
	 */
	public final static int MACROLIB_USB_VENDOR_ID = 0xee55;

	final static System.Logger LOG = System.getLogger(UInput.class.getName());

	/**
	 * Find the actual input device given the virtual device type.
	 *
	 * @param deviceType device type
	 * @return path to device
	 * @throws IOException on error
	 */
	public static Path getDevice(TargetType deviceType) throws IOException {
		Path viPath = Paths.get("/sys/devices/virtual/input");
		if (Files.exists(viPath)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(viPath)) {
				for (Path devDir : stream) {
					Path nameFile = devDir.resolve("name");
					if (Files.exists(nameFile)) {
						String deviceName = null;
						try (BufferedReader r = Files.newBufferedReader(nameFile)) {
							deviceName = r.readLine().replace("\n", "");
						}
						if (deviceName.equals("gnome15-" + deviceType.name().toLowerCase())) {
							try (DirectoryStream<Path> innerStream = Files.newDirectoryStream(devDir)) {
								for (Path dp : innerStream) {
									if (dp.getFileName().toString().startsWith("js"))
										return Paths.get("/dev/input/" + dp.getFileName().toString());
								}
								for (Path dp : innerStream) {
									if (dp.getFileName().toString().startsWith("event"))
										return Paths.get("/dev/input/" + dp.getFileName().toString());
								}
							}
						}
					}
				}
			}
		}
		throw new FileNotFoundException(String.format("No device for type %s.", deviceType));
	}

	/**
	 * Test for the existence of calibration tools 'jstest-gtk' and 'jscal'.
	 *
	 * @return true, if is calibrate available
	 */
	public static boolean isCalibrateAvailable() {
		try {
			return run("which", "jstest-gtk") == 0 && run("which", "jscal") == 0;
		} catch (IOException ioe) {
			return false;
		}
	}

	static int run(String... cmd) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		InputStream in = p.getInputStream();
		in.transferTo(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		});
		try {
			return p.waitFor();
		} catch (InterruptedException e) {
			return -1;
		}
	}

	private Map<String, String> keysymMap = new HashMap<>();

//	private final static Map<TargetType, Map<EventCode, List<Integer>>> registeredParameters = new HashMap<>();
//
//	static {
//		Map<EventCode, List<Integer>> js = new HashMap<>();
//		js.put(EventCode.ABS_X, Arrays.asList(JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//		js.put(EventCode.ABS_Y, Arrays.asList(JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//
//		Map<EventCode, List<Integer>> js2 = new HashMap<>();
//		js2.put(EventCode.ABS_X, Arrays.asList(JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//		js2.put(EventCode.ABS_Y, Arrays.asList(JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//
//		registeredParameters.put(TargetType.MOUSE, new HashMap<>());
//		registeredParameters.put(TargetType.JOYSTICK, js);
//		registeredParameters.put(TargetType.DIGITAL_JOYSTICK, js2);
//	}
//
//	public static Map<TargetType, Map<EventCode, List<Integer>>> getRegisteredParameters() {
//		return registeredParameters;
//	}

	private Map<TargetType, Semaphore> locks = new HashMap<>();

	private MacroSystem macroSystem;

	private Map<TargetType, InputDevice> uinputDevices = new HashMap<TargetType, InputDevice>();

	/**
	 * Instantiates a new u input.
	 *
	 * @param macroSystem the macro system
	 */
	public UInput(MacroSystem macroSystem) {
		this.macroSystem = macroSystem;

		for (TargetType t : TargetType.values())
			if (t.isUInput())
				locks.put(t, new Semaphore(1));

	}

	/**
	 * Run external joystick calibration utility.
	 *
	 * @param deviceType device type
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void calibrate(TargetType deviceType) throws IOException {
		if (isCalibrateAvailable()
				&& (deviceType == TargetType.JOYSTICK || deviceType == TargetType.DIGITAL_JOYSTICK)) {
			Path device_file = getDevice(deviceType);
			loadCalibration(deviceType);
			run("jstest-gtk", device_file.toString());
			saveCalibration(deviceType);
		} else
			throw new IllegalArgumentException(String.format("Cannot calibrate this device type (%s)", deviceType));
	}

	/**
	 * Clean up, closing all the devices.
	 *
	 * @throws IOException on error
	 */
	@Override
	public void close() throws IOException {
		for (TargetType deviceType : TargetType.values()) {
			if (deviceType.isUInput()) {
				LOG.log(Level.DEBUG, String.format("Closing UINPUT device %s", deviceType));
				uinputDevices.remove(deviceType).close();
			}
		}
	}

	/**
	 * Emit an input event, optionally emit a SYN as well.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 * @param evt    the evt
	 * @param syn    emit SYN (defaults to True)
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void emit(TargetType target, Event evt, boolean syn) throws IOException {
		if (!target.isUInput())
			throw new IllegalArgumentException(
					String.format("Invalid target. '%s' must be one of %s", target, TargetType.uinputTypes()));

		LOG.log(Level.DEBUG, "UINPUT uinput keyboard event at %s, code = %s, val = %d, syn = %s", target, evt.getCode(),
				evt.getValue(), String.valueOf(syn));

		try {
			locks.get(target).acquire();
			try {
				uinputDevices.get(target).emit(evt, syn);
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
			throw new IOException("Interrupted acquiring device.");
		}
	}

	/**
	 * Emit an input event followed by a SYNC.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 *               This just determines the device it will be output to, not
	 *               native event type
	 * @param code   uinput code
	 * @param value  uinput value
	 * @throws IOException on error
	 */
	public void emit(TargetType target, EventCode code, int value) throws IOException {
		emit(target, code, value, true);
	}

	/**
	 * Emit an input event, optionally emit a SYN as well.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 *               This just determines the device it will be output to, not
	 *               native event type
	 * @param code   uinput code
	 * @param value  uinput value
	 * @param syn    emit SYN (defaults to True)
	 * @throws IOException on error
	 */
	public void emit(TargetType target, EventCode code, int value, boolean syn) throws IOException {
		try {
			locks.get(target).acquire();
			try {
				doEmit(target, code, value, syn);
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Emit an input event followed by a SYN.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 * @param code   uinput code (either number code, where type will be determined
	 *               by target or a the event code name)
	 * @param value  uinput value
	 * @throws IOException on error
	 */
	public void emit(TargetType target, String code, int value) throws IOException {
		try {
			locks.get(target).acquire();
			try {
				doEmit(target, EventCode.parse(code), value, true);
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Emit an input event, optionally emit a SYN as well.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 * @param code   uinput code (either single code, where type will be determined
	 *               by target or a tuple consisting of event type and event code)
	 * @param value  uinput value
	 * @param syn    emit SYN (defaults to True)
	 * @throws IOException on error
	 */
	public void emit(TargetType target, String code, int value, boolean syn) throws IOException {
		try {
			locks.get(target).acquire();
			try {
				doEmit(target, EventCode.parse(code), value, syn);
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Get the mapping for the provided keysym. This is case insensitive. Keyword
	 * arguments:
	 *
	 * @param keysym X keysym
	 * @return the keysym TO uinput mapping
	 */
	public String getKeysymTOUinputMapping(String keysym) {
		if (keysymMap.containsKey(keysym.toLowerCase()))
			return keysymMap.get(keysym.toLowerCase());
		throw new IllegalArgumentException(String.format("Failed to translate X keysym %s to UInput code.", keysym));
	}

	/**
	 * Run external joystick calibration utility.
	 *
	 * @param deviceType device type
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void loadCalibration(TargetType deviceType) throws IOException {
		if (isCalibrateAvailable()
				&& (deviceType == TargetType.JOYSTICK || deviceType == TargetType.DIGITAL_JOYSTICK)) {
			Path jsConfigFile = getJsConfigFile(deviceType);
			if (Files.exists(jsConfigFile)) {
				try (BufferedReader r = Files.newBufferedReader(jsConfigFile)) {
					String[] cal = r.readLine().trim().split("\\s+");
					LOG.log(Level.DEBUG, String.format("Calibrating using '%s'", Arrays.asList(cal)));
					run(cal);
					LOG.log(Level.DEBUG, String.format("Calibrated using '%s'", Arrays.asList(cal)));
				} catch (Exception e) {
					LOG.log(Level.ERROR, "Failed to calibrate joystick device.", e);
				}
			} else {
				LOG.log(Level.WARNING, "No joystick calibration available.");
			}
		} else
			throw new IllegalArgumentException(String.format("Cannot calibrate this device type (%s)", deviceType));
	}

	/**
	 * Initialize, opening all devices.
	 *
	 * @throws IOException on error
	 */
	public void open() throws IOException {

		for (TargetType deviceType : TargetType.values()) {
			if (deviceType.isUInput() && !uinputDevices.containsKey(deviceType)) {
				LOG.log(Level.DEBUG, String.format("Opening uinput device for %s.", deviceType));

				InputDevice uinputDevice;
				int virtual_product_id;

				if (deviceType == TargetType.MOUSE) {
					virtual_product_id = MACROLIB_MOUSE_PRODUCT_ID;
					uinputDevice = new InputDevice(String.format("macrolib-%s", deviceType), MACROLIB_USB_VENDOR_ID,
							virtual_product_id);
					addCaps(deviceType, uinputDevice);
					// TODO hmm... calibration?
//	                keys.append((REL_X[0], REL_X[1], 0, 255, 0, 0));
//	                keys.append((REL_Y[0], REL_Y[1], 0, 255, 0, 0));
				} else if (deviceType == TargetType.JOYSTICK) {
					virtual_product_id = MACROLIB_JOYSTICK_PRODUCT_ID;
					uinputDevice = new InputDevice(String.format("macrolib-%s", deviceType), MACROLIB_USB_VENDOR_ID,
							virtual_product_id);
					uinputDevice.addCapability(EventCode.ABS_X, EventCode.ABS_Y);
					addCaps(deviceType, uinputDevice);
					// TODO hmm... calibration?
//	                keys.append(EventCode.ABS_X + (JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//	                keys.append(EventCode.ABS_Y + (JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
				} else if (deviceType == TargetType.DIGITAL_JOYSTICK) {
					virtual_product_id = MACROLIB_JOYSTICK_PRODUCT_ID;
					uinputDevice = new InputDevice(String.format("macrolib-%s", deviceType), MACROLIB_USB_VENDOR_ID,
							virtual_product_id);
					uinputDevice.addCapability(EventCode.ABS_X, EventCode.ABS_Y);
					addCaps(deviceType, uinputDevice);
//	                keys.append(EventCode.ABS_X + (JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
//	                keys.append(EventCode.ABS_Y + (JOYSTICK_MIN, JOYSTICK_MAX, 0, 0));
				} else {
					virtual_product_id = MACROLIB_KEYBOARD_PRODUCT_ID;
					uinputDevice = new InputDevice(String.format("macrolib-%s", deviceType), MACROLIB_USB_VENDOR_ID,
							virtual_product_id);
					addCaps(deviceType, uinputDevice);
				}

				uinputDevice.open();
				uinputDevices.put(deviceType, uinputDevice);

				// Centre the joystick by default
				if (deviceType == TargetType.JOYSTICK || deviceType == TargetType.DIGITAL_JOYSTICK) {
					syn(deviceType);
					if(isCalibrateAvailable())
						loadCalibration(deviceType);
					else
						LOG.log(Level.WARNING, String.format("Virtual device %s cannot be calibrated as calibration tools are not installed.", deviceType));
					emit(deviceType, EventCode.ABS_X, JOYSTICK_CENTER, false);
					emit(deviceType, EventCode.ABS_Y, JOYSTICK_CENTER, false);
					syn(deviceType);
				} else {
					emit(deviceType, EventCode.KEY_RESERVED, 0, true);
					emit(deviceType, EventCode.KEY_RESERVED, 1, true);
				}
			}
		}
	}

	/**
	 * Run external joystick calibration utility.
	 *
	 * @param deviceType device type
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void saveCalibration(TargetType deviceType) throws IOException {
		if (isCalibrateAvailable()
				&& (deviceType == TargetType.JOYSTICK || deviceType == TargetType.DIGITAL_JOYSTICK)) {
			Path deviceFile = getDevice(deviceType);
			Path jsConfigFile = getJsConfigFile(deviceType);
			ProcessBuilder pb = new ProcessBuilder("jscal", "-q", deviceFile.toString());
			Process p = pb.start();
			try (InputStream in = p.getInputStream()) {
				try (OutputStream out = Files.newOutputStream(jsConfigFile)) {
					in.transferTo(out);
				}
			}
		} else
			throw new IllegalArgumentException(String.format("Cannot calibrate this device type (%s)", deviceType));
	}

	/**
	 *
	 * Emit the syn.
	 * 
	 * @param target target device type (MOUSE, KEYBOARD or JOYSTICK).
	 * @throws IOException on error
	 */
	public void syn(TargetType target) throws IOException {
		try {
			locks.get(target).acquire();
			try {
				uinputDevices.get(target).syn();
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Type an input event (a press followed by a release), optionally emit a SYN as
	 * well.
	 *
	 * @param target The target device type (MOUSE, KEYBOARD or JOYSTICK) type code.
	 *               This just determines the device it will be output to, not
	 *               native event type
	 * @param code   uinput code
	 * @throws IOException on error
	 */
	public void type(TargetType target, EventCode code) throws IOException {
		try {
			locks.get(target).acquire();
			try {
				doEmit(target, code, 1, true);
				doEmit(target, code, 0, true);
			} finally {
				locks.get(target).release();
			}
		} catch (InterruptedException ie) {
		}
	}

	protected void doEmit(TargetType target, EventCode code, int value, boolean syn) throws IOException {
		if (!target.isUInput())
			throw new IllegalArgumentException(
					String.format("Invalid target. '%s' must be one of %s", target, TargetType.uinputTypes()));

		Event evt = null;
		if (target == TargetType.MOUSE && (code == EventCode.REL_X || code == EventCode.REL_Y)) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("UINPUT mouse event at %s, code = %s, val = %d, syn = %s", target,
						code, value, String.valueOf(syn)));
		} else if (target == TargetType.JOYSTICK || target == TargetType.DIGITAL_JOYSTICK) {
			/* We translate the 'virtual' uinput codes into real uinput ones */
			if (code == EventCode.BTN_DPAD_LEFT) {
				value = value > 0 ? JOYSTICK_MIN : JOYSTICK_CENTER;
				code = EventCode.ABS_X;
			} else if (code == EventCode.BTN_DPAD_RIGHT) {
				value = value > 0 ? JOYSTICK_MAX : JOYSTICK_CENTER;
				code = EventCode.ABS_X;
			} else if (code == EventCode.BTN_DPAD_UP) {
				value = value > 0 ? JOYSTICK_MIN : JOYSTICK_CENTER;
				code = EventCode.ABS_Y;
			} else if (code == EventCode.BTN_DPAD_DOWN) {
				value = value > 0 ? JOYSTICK_MAX : JOYSTICK_CENTER;
				code = EventCode.ABS_Y;
			} else {
				/* If we are simulating a button press, then the event is of type EV_KEY */
			}
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("UINPUT joystick event at %s, code = %s, val = %d, syn = %s", target,
						code, value, String.valueOf(syn)));
		}
		evt = new Event(code, value);
		uinputDevices.get(target).emit(evt, syn);
	}

	Set<EventCode> getButtons(TargetType deviceType) throws IOException {
		String fname = String.format("%s.keys", deviceType.name().toLowerCase().replace("_", "-"));
		Set<EventCode> b = new LinkedHashSet<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(UInput.class.getResourceAsStream(fname)))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (!line.equals("") && !line.startsWith("#")) {
					try {
						EventCode code = EventCode.parse(line);
						b.add(code);
					} catch (IllegalArgumentException iae) {
						throw new IOException(String.format("Failed to parse %s", fname), iae);
					}
				}
			}
		}
		return b;
	}

	private void addCaps(TargetType deviceType, InputDevice uinputDevice) throws IOException {
		for (EventCode b : getButtons(deviceType)) {
			uinputDevice.addCapability(b);
		}
	}

	/**
	 * 
	 * Returns the filename used for saving the joystick calibration file
	 * 
	 * If the directory that should own the file doesn't exist, it will be created.
	 * 
	 * @param deviceType device type
	 */
	private Path getJsConfigFile(TargetType deviceType) {
		return macroSystem.getWriter().getConfiguration()
				.resolve(String.format("%s.js", deviceType.name().toLowerCase()));
	}

//	public List<List<Integer>> getKeys(TargetType deviceType) {
//		switch (deviceType) {
//		case MOUSE:
//			return getKeys("BTN_", "BTN_TOOL_");
//		case JOYSTICK:
//			return getKeys("BTN_", "X_", "Y_");
//		default:
//			return getKeys("KEY_");
//		}
//	}
//
//	private List<List<Integer>> getKeys(String prefix, String... exclude) {
//		List<List<Integer>> l = new ArrayList<>();
//		for (String k : sortSet(caps.keySet())) {
//			if (k.startsWith(prefix) && (exclude == null || !startsWith(k, exclude)))
//				l.add(caps.get(k));
//		}
//		return l;
//	}

//	private boolean startsWith(String k, String[] l) {
//		for (String e : l) {
//			if (k.startsWith(e))
//				return true;
//		}
//		return false;
//	}
//
//	private Set<String> sortSet(Set<String> keySet) {
//		Set<String> l = new LinkedHashSet<>();
//		List<String> s = new ArrayList<>(keySet);
//		Collections.sort(s);
//		l.addAll(s);
//		return l;
//	}

}