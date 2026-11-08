package uk.co.bithatch.macrolib;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Type;

/**
 * The Enum TargetType.
 */
public enum TargetType {
	ACTION, COMMAND, DIGITAL_JOYSTICK, JOYSTICK, KEYBOARD, MOUSE, NOTHING, SCRIPT, SIMPLE;

	/**
	 * Uinput types.
	 *
	 * @return the target type[]
	 */
	public static TargetType[] uinputTypes() {
		return new TargetType[] { MOUSE, KEYBOARD, JOYSTICK, DIGITAL_JOYSTICK };
	}

	/**
	 * Checks if is u input.
	 *
	 * @return true, if is u input
	 */
	public boolean isUInput() {
		switch (this) {
		case MOUSE:
		case KEYBOARD:
		case JOYSTICK:
		case DIGITAL_JOYSTICK:
			return true;
		default:
			return false;
		}
	}

	/**
	 * To U input type.
	 *
	 * @return the type
	 */
	public Type toUInputType() {
		switch (this) {
		case MOUSE:
		case KEYBOARD:
			return Type.EV_KEY;
		case JOYSTICK:
			return Type.EV_ABS;
		case DIGITAL_JOYSTICK:
			return Type.EV_REL;
		default:
			throw new IllegalArgumentException();
		}
	}

	public static TargetType forEvent(EventCode event) {
		switch (event.type()) {
		case EV_ABS:
			return TargetType.JOYSTICK;
		case EV_REL:
			return TargetType.MOUSE;
		case EV_KEY:
			return event.isButton() ? TargetType.MOUSE : TargetType.KEYBOARD;
		default:
			throw new IllegalArgumentException();
		}
	}
}