package uk.co.bithatch.snake.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.input.KeyCode;
import uk.co.bithatch.snake.lib.OnboardProfiles.Action;

/** Names for onboard button actions and keys. */
final class OnboardText {

	private OnboardText() {
	}

	static final Map<Integer, String> MOUSE_NAMES = new LinkedHashMap<>();
	static {
		MOUSE_NAMES.put(1, "Left click");
		MOUSE_NAMES.put(2, "Right click");
		MOUSE_NAMES.put(3, "Middle click");
		MOUSE_NAMES.put(4, "Back");
		MOUSE_NAMES.put(5, "Forward");
		MOUSE_NAMES.put(9, "Scroll left");
		MOUSE_NAMES.put(10, "Scroll right");
	}

	static String describe(Action a) {
		if (a == null)
			return "?";
		byte[] p = a.params();
		switch (a.type()) {
		case Action.DISABLED:
			return "Disabled";
		case Action.MOUSE:
			return p.length > 0 ? MOUSE_NAMES.getOrDefault(p[0] & 0xff, "Mouse button " + (p[0] & 0xff)) : "Mouse";
		case Action.KEY: {
			var sb = new StringBuilder();
			int m = a.keyModifiers();
			if ((m & 0x11) != 0)
				sb.append("Ctrl+");
			if ((m & 0x22) != 0)
				sb.append("Shift+");
			if ((m & 0x44) != 0)
				sb.append("Alt+");
			if ((m & 0x88) != 0)
				sb.append("Super+");
			return sb + HidKeys.name(a.keyUsage());
		}
		case Action.MACRO:
		case Action.MACRO_REPEAT:
			return "Macro (from Synapse)";
		case Action.DPI:
			return p.length > 0 && p[0] == 2 ? "DPI down" : "DPI up";
		case Action.PROFILE:
			return "Next profile";
		case Action.SCROLL:
			return "Scroll";
		default:
			return String.format("Other (%02x)", a.type());
		}
	}

	/** USB HID keyboard usages for JavaFX key codes. */
	static final class HidKeys {
		private static final Map<KeyCode, Integer> USAGE = new LinkedHashMap<>();
		private static final Map<Integer, String> NAMES = new LinkedHashMap<>();

		private static void put(KeyCode k, int usage, String name) {
			USAGE.put(k, usage);
			NAMES.putIfAbsent(usage, name);
		}

		static {
			for (int i = 0; i < 26; i++) {
				var k = KeyCode.valueOf(String.valueOf((char) ('A' + i)));
				put(k, 0x04 + i, k.getName());
			}
			KeyCode[] digits = { KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
					KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9, KeyCode.DIGIT0 };
			for (int i = 0; i < 10; i++)
				put(digits[i], 0x1e + i, String.valueOf((i + 1) % 10));
			put(KeyCode.ENTER, 0x28, "Enter");
			put(KeyCode.ESCAPE, 0x29, "Esc");
			put(KeyCode.BACK_SPACE, 0x2a, "Backspace");
			put(KeyCode.TAB, 0x2b, "Tab");
			put(KeyCode.SPACE, 0x2c, "Space");
			put(KeyCode.MINUS, 0x2d, "-");
			put(KeyCode.EQUALS, 0x2e, "=");
			put(KeyCode.OPEN_BRACKET, 0x2f, "[");
			put(KeyCode.CLOSE_BRACKET, 0x30, "]");
			put(KeyCode.BACK_SLASH, 0x31, "\\");
			put(KeyCode.SEMICOLON, 0x33, ";");
			put(KeyCode.QUOTE, 0x34, "'");
			put(KeyCode.BACK_QUOTE, 0x35, "`");
			put(KeyCode.COMMA, 0x36, ",");
			put(KeyCode.PERIOD, 0x37, ".");
			put(KeyCode.SLASH, 0x38, "/");
			put(KeyCode.CAPS, 0x39, "CapsLock");
			for (int i = 0; i < 12; i++)
				put(KeyCode.valueOf("F" + (i + 1)), 0x3a + i, "F" + (i + 1));
			put(KeyCode.PRINTSCREEN, 0x46, "PrintScreen");
			put(KeyCode.SCROLL_LOCK, 0x47, "ScrollLock");
			put(KeyCode.PAUSE, 0x48, "Pause");
			put(KeyCode.INSERT, 0x49, "Insert");
			put(KeyCode.HOME, 0x4a, "Home");
			put(KeyCode.PAGE_UP, 0x4b, "PageUp");
			put(KeyCode.DELETE, 0x4c, "Delete");
			put(KeyCode.END, 0x4d, "End");
			put(KeyCode.PAGE_DOWN, 0x4e, "PageDown");
			put(KeyCode.RIGHT, 0x4f, "Right");
			put(KeyCode.LEFT, 0x50, "Left");
			put(KeyCode.DOWN, 0x51, "Down");
			put(KeyCode.UP, 0x52, "Up");
			put(KeyCode.NUM_LOCK, 0x53, "NumLock");
			put(KeyCode.DIVIDE, 0x54, "Num /");
			put(KeyCode.MULTIPLY, 0x55, "Num *");
			put(KeyCode.SUBTRACT, 0x56, "Num -");
			put(KeyCode.ADD, 0x57, "Num +");
			KeyCode[] num = { KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3, KeyCode.NUMPAD4, KeyCode.NUMPAD5,
					KeyCode.NUMPAD6, KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9, KeyCode.NUMPAD0 };
			for (int i = 0; i < 10; i++)
				put(num[i], 0x59 + i, "Num " + ((i + 1) % 10));
			put(KeyCode.DECIMAL, 0x63, "Num .");
			put(KeyCode.CONTEXT_MENU, 0x65, "Menu");
			for (int i = 12; i < 24; i++)
				put(KeyCode.valueOf("F" + (i + 1)), 0x68 + i - 12, "F" + (i + 1));
		}

		static int usage(KeyCode k) {
			return USAGE.getOrDefault(k, 0);
		}

		static String name(int usage) {
			return NAMES.getOrDefault(usage, String.format("Key %02x", usage));
		}
	}
}
