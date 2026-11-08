package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.KeySym;
import com.sun.jna.platform.unix.X11.WindowByReference;
import com.sun.jna.platform.unix.X11.XEvent;
import com.sun.jna.platform.unix.X11.XKeyEvent;
import com.sun.jna.ptr.IntByReference;

/**
 * The Class X11DesktopIO.
 */
public class X11DesktopIO implements DesktopIO {

	/**
	 * The Interface XLib.
	 */
	public interface XLib extends X11 {
		XLib INSTANCE = Native.load("X11", XLib.class);

		/**
		 * X get input focus.
		 *
		 * @param display          the display
		 * @param focus_return     the focus return
		 * @param revert_to_return the revert to return
		 */
		void XGetInputFocus(X11.Display display, X11.WindowByReference focus_return, IntByReference revert_to_return);
	}

	final static Logger LOG = System.getLogger(X11DesktopIO.class.getName());

	private final static Map<String, String> special_X_keysyms = new HashMap<>();
	static {
		special_X_keysyms.put(" ", "space");
		special_X_keysyms.put("\t", "Tab");
		special_X_keysyms.put("\n", "Return"); // for some reason this needs to be cr, not lf
		special_X_keysyms.put("\r", "Return");
		special_X_keysyms.put(String.valueOf((char) 0x1b), "Escape");
		special_X_keysyms.put("\b", "BackSpace");
		special_X_keysyms.put("!", "exclam");
		special_X_keysyms.put("#", "numbersign");
		special_X_keysyms.put("%", "percent");
		special_X_keysyms.put("$", "dollar");
		special_X_keysyms.put("&", "ampersand");
		special_X_keysyms.put("\"", "quotedbl");
		special_X_keysyms.put("\"", "apostrophe");
		special_X_keysyms.put("(", "parenleft");
		special_X_keysyms.put(")", "parenright");
		special_X_keysyms.put("*", "asterisk");
		special_X_keysyms.put("=", "equal");
		special_X_keysyms.put("+", "plus");
		special_X_keysyms.put(",", "comma");
		special_X_keysyms.put("-", "minus");
		special_X_keysyms.put(".", "period");
		special_X_keysyms.put("/", "slash");
		special_X_keysyms.put(":", "colon");
		special_X_keysyms.put(";", "semicolon");
		special_X_keysyms.put("<", "less");
		special_X_keysyms.put(">", "greater");
		special_X_keysyms.put("?", "question");
		special_X_keysyms.put("@", "at");
		special_X_keysyms.put("[", "bracketleft");
		special_X_keysyms.put("]", "bracketright");
		special_X_keysyms.put("\\", "backslash");
		special_X_keysyms.put("^", "asciicircum");
		special_X_keysyms.put("_", "underscore");
		special_X_keysyms.put("`", "grave");
		special_X_keysyms.put("{", "braceleft");
		special_X_keysyms.put("|", "bar");
		special_X_keysyms.put("}", "braceright");
		special_X_keysyms.put("~", "asciitilde");
	}
	// private Window window;
	private Display localDpy;
	private IntByReference maxKeycodes;

	private IntByReference minKeycodes;

	private boolean useXTest = true;

	private WindowByReference winRef;

	private boolean xTestAvailable;

	/**
	 * Instantiates a new x 11 desktop IO.
	 */
	public X11DesktopIO() {
		SwingUtilities.invokeLater(() -> {
			initXtest();
		});
	}

	/**
	 * Type string.
	 *
	 * @param string the string
	 * @param press  the press
	 */
	@Override
	public void typeString(String string, boolean press) {
		if (special_X_keysyms.containsKey(string)) {
			string = special_X_keysyms.get(string);
		}
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, "Sending string %s", string);

		if (!useXTest || !xTestAvailable) {
			winRef = new X11.WindowByReference();
			IntByReference revert_to_return = new IntByReference();
			X11DesktopIO.XLib.INSTANCE.XGetInputFocus(localDpy, winRef, revert_to_return);
		}
		int[] kcs = charToKeycodes(string);

		if (kcs == null) {
			LOG.log(Level.WARNING, String.format("Could not map keysym %s to keycodes", string));
			return;
		}

		int keycode = kcs[0];
		int shift_mask = kcs[1];
//		int keysym = kcs[2];

		if (xTestAvailable && useXTest) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("XTEST Sending keychar %s keycode %d, press = %s, shift = %d",
						string, keycode, String.valueOf(press), shift_mask));
			if (press) {
				if (shift_mask != 0)
					X11.XTest.INSTANCE.XTestFakeKeyEvent(localDpy, 62, true, new NativeLong(0));
				X11.XTest.INSTANCE.XTestFakeKeyEvent(localDpy, keycode, true, new NativeLong(0));
			} else {
				X11.XTest.INSTANCE.XTestFakeKeyEvent(localDpy, keycode, false, new NativeLong(0));
				if (shift_mask != 0)
					X11.XTest.INSTANCE.XTestFakeKeyEvent(localDpy, 62, false, new NativeLong(0));

				/* Not reading so must flush */
				X11.INSTANCE.XFlush(localDpy);
			}
		} else {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Raw X11 Sending keychar %s keycode %d, press = %s, shift = %d",
						string, keycode, String.valueOf(press), shift_mask));
			int screen = X11.INSTANCE.XDefaultScreen(localDpy);
			int ret;
			if (press) {
				XKeyEvent xkev = createXKeyEvent(X11.KeyPress, keycode, shift_mask, screen);
				XEvent event = new XEvent();
				event.xkey = xkev;
				NativeLong mask = new NativeLong(X11.KeyPressMask);
				ret = X11.INSTANCE.XSendEvent(localDpy, winRef.getValue(), 1, mask, event);
			} else {
				XKeyEvent xkev = createXKeyEvent(X11.KeyRelease, keycode, shift_mask, screen);
				XEvent event = new XEvent();
				event.xkey = xkev;
				NativeLong mask = new NativeLong(X11.KeyReleaseMask);
				ret = X11.INSTANCE.XSendEvent(localDpy, winRef.getValue(), 1, mask, event);
			}
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG,
						String.format("Raw X11 Sent keychar %s keycode %d, press = %s, shift = %d, resulted in %d",
								string, keycode, String.valueOf(press), shift_mask, ret));
			X11.INSTANCE.XFlush(localDpy);
//                X11.INSTANCE.XSync(localDpy, false);
		}
	}

	/**
	 * 
	 * Convert a character from a string into an X11 keycode when possible.
	 * 
	 * @param ch character to convert
	 * @return keycodes
	 */
	int[] charToKeycodes(String ch) {
		int shift_mask = 0;
		int keycode;
		int keysym_code;

		if (ch.startsWith("[")) {
			keysym_code = Integer.parseInt(ch.substring(1, ch.length() - 2));
			// AltGr
			if (keysym_code == 65027)
				keycode = 108;
			else {
				LOG.log(Level.WARNING, String.format("Unknown keysym %d", keysym_code));
				keycode = 0;
			}
		} else {

			KeySym keysym = getKeysym(ch);

			keysym_code = keysym == null ? 0 : keysym.intValue();
			keycode = keysym_code == 0 ? 0 : X11.INSTANCE.XKeysymToKeycode(localDpy, keysym);

			KeySym kctos = X11.INSTANCE.XKeycodeToKeysym(localDpy, (byte) keycode, 0);
			if (kctos == null || kctos.intValue() != keysym_code) {
				kctos = X11.INSTANCE.XKeycodeToKeysym(localDpy, (byte) keycode, 1);
				if (kctos != null && kctos.intValue() == keysym_code) {
					shift_mask = X11.ShiftMask;
				} else {
					// TODO Some cluesh ere ?
					// http://git.yoctoproject.org/cgit/cgit.cgi/libfakekey/tree/src/libfakekey.c
					return null;
				}
			}

//			Integer[] x_keycodes = keysymToKeyCodes(keysym);
//			LOG.log(Level.INFO, String.format("Keycodes for " + ch + " " + keysym_code + " are " + Arrays.asList(x_keycodes)));
//	          keycode = 0 if keysym == 0 else self.local_dpy.keysym_to_keycode(keysym);
//				/*
//				 * I have no idea how accurate this is, but it seems more so that the
//				 * is_shifted() function
//				 */
//			if (keysym_code < 256) {
//				for (int x : x_keycodes) {
//					if (x == 1)
//						shift_mask = X11.ShiftMask;
//				}
//			}
		}

		if (keycode == 0)
			LOG.log(Level.WARNING, String.format("Sorry, can't map (character %s)", ch));

		return new int[] { keycode, shift_mask, keysym_code };
	}

	XKeyEvent createXKeyEvent(int type, int keycode, int shift_mask, int screen) {
		XKeyEvent xkev = new XKeyEvent();
		xkev.type = type;
		xkev.display = localDpy;
		xkev.serial = new NativeLong(0l);
		xkev.send_event = 1;
		xkev.time = new NativeLong(System.currentTimeMillis());
		xkev.root = X11.INSTANCE.XRootWindow(localDpy, screen);
		xkev.window = winRef.getValue();
		xkev.same_screen = 0;
		xkev.state = shift_mask;
		xkev.keycode = keycode;
		xkev.subwindow = null;
		xkev.x_root = 0;
		xkev.y_root = 0;
		xkev.x = 0;
		xkev.y = 0;
		return xkev;
	}

	KeySym getKeysym(String ch) {
		KeySym keysym = X11.INSTANCE.XStringToKeysym(ch);
		if (keysym == null) {
			/*
			 * Unfortunately, although this works to get the correct keysym i.e. keysym for
			 * '#' is returned as "numbersign" the subsequent
			 * display.keysym_to_keycode("numbersign") is 0.
			 */
			if (special_X_keysyms.containsKey(ch)) {
				keysym = X11.INSTANCE.XStringToKeysym(special_X_keysyms.get(ch));
			}
		}
		return keysym;
	}

	/**
	 * 
	 * Initialise XTEST if it is available.
	 */
	void initXtest() {
		LOG.log(Level.DEBUG, "Initialising macro output system");

		/* Load Virtkey if it is available, using it for preference */

		/* Determine whether to use XTest for sending key events to X */
		xTestAvailable = true;
		try {
			@SuppressWarnings("unused")
			Object o = X11.XTest.INSTANCE;
		} catch (Exception e2) {
			LOG.log(Level.DEBUG, "No XTest, falling back to raw X11 events", e2);
			xTestAvailable = false;
		}

		localDpy = X11.INSTANCE.XOpenDisplay(null);
		minKeycodes = new IntByReference();
		maxKeycodes = new IntByReference();
		X11.INSTANCE.XDisplayKeycodes(localDpy, minKeycodes, maxKeycodes);

		if (xTestAvailable && !X11.XTest.INSTANCE.XTestQueryExtension(localDpy, new IntByReference(),
				new IntByReference(), new IntByReference(), new IntByReference())) {
			LOG.log(Level.DEBUG, "Found XTEST module, but the X extension could not be found");
			xTestAvailable = false;
		}
	}

	Integer[] keysymToKeyCodes(KeySym keysym) {
		List<Integer> l = new ArrayList<>();
		for (int i = minKeycodes.getValue(); i <= maxKeycodes.getValue(); i++) {
			for (int j = 0; j < 8; j++) {
				if (X11.INSTANCE.XKeycodeToKeysym(localDpy, (byte) i, j) == keysym) {
					l.add(i);
					break;
				}
			}
		}
		return l.toArray(new Integer[0]);
	}
}
