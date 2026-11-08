package uk.co.bithatch.snake.ui.tray;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import dorkbox.systemTray.SystemTray;

/**
 * Cinnamon shows AppIndicators, but always opens their menu on any click. XApp
 * status icons (Cinnamon's own tray API) tell left and right clicks apart. So
 * on Cinnamon this hides dorkbox's AppIndicator and shows an XApp icon in its
 * place: left click runs an action (open Snake), right click pops up the very
 * same GTK menu dorkbox built for the indicator.
 */
class XAppTray {
	final static Logger LOG = System.getLogger(XAppTray.class.getName());

	public interface XApp extends Library {
		Pointer xapp_status_icon_new();

		void xapp_status_icon_set_name(Pointer icon, String name);

		void xapp_status_icon_set_icon_name(Pointer icon, String iconName);

		void xapp_status_icon_set_tooltip_text(Pointer icon, String text);

		/* gboolean: must be int, a Java boolean arrives as FALSE */
		void xapp_status_icon_set_visible(Pointer icon, int visible);

		void xapp_status_icon_popup_menu(Pointer icon, Pointer menu, int x, int y, int button, int time,
				int panelPosition);
	}

	public interface GLib extends Library {
		long g_signal_connect_data(Pointer instance, String signal, Callback handler, Pointer data,
				Pointer destroy, int flags);

		void g_object_unref(Pointer object);
	}

	public interface Indicator extends Library {
		Pointer app_indicator_get_menu(Pointer indicator);

		void app_indicator_set_status(Pointer indicator, int status);
	}

	/** button-release-event (icon, x, y, button, time, panel_position, data) */
	public interface ButtonReleaseCallback extends Callback {
		void callback(Pointer icon, int x, int y, int button, int time, int panelPosition, Pointer data);
	}

	private static final int APP_INDICATOR_STATUS_PASSIVE = 0;

	private final SystemTray systemTray;
	private final Runnable onLeftClick;
	private final XApp xapp;
	private final GLib glib;
	private final Indicator indicator;
	/* Held in a field so JNA's callback isn't garbage collected */
	private final ButtonReleaseCallback releaseCallback;
	private Pointer icon;

	static boolean isSupported() {
		if (!Boolean.parseBoolean(System.getProperty("snake.xappTray", "true")))
			return false;
		var desktop = System.getenv("XDG_CURRENT_DESKTOP");
		return desktop != null && desktop.toLowerCase().contains("cinnamon");
	}

	XAppTray(SystemTray systemTray, String title, Runnable onLeftClick) {
		this.systemTray = systemTray;
		this.onLeftClick = onLeftClick;
		xapp = Native.load("xapp", XApp.class);
		glib = Native.load("gobject-2.0", GLib.class);
		indicator = Native.load("ayatana-appindicator3", Indicator.class);
		releaseCallback = (i, x, y, button, time, panelPosition, data) -> {
			try {
				if (button == 1) {
					onLeftClick.run();
				} else if (button == 3) {
					var ind = indicatorPointer();
					var menu = ind == null ? null : indicator.app_indicator_get_menu(ind);
					if (menu != null)
						xapp.xapp_status_icon_popup_menu(icon, menu, x, y, button, time, panelPosition);
				}
			} catch (Throwable t) {
				LOG.log(Level.WARNING, "Tray click failed.", t);
			}
		};
		dispatch(() -> {
			icon = xapp.xapp_status_icon_new();
			xapp.xapp_status_icon_set_name(icon, title);
			xapp.xapp_status_icon_set_tooltip_text(icon, title);
			glib.g_signal_connect_data(icon, "button-release-event", releaseCallback, null, null, 0);
		});
		sync(true);
	}

	/**
	 * Call after anything that may have changed the dorkbox indicator (image,
	 * enabled). Runs on the GTK thread after dorkbox's own queued work, hides the
	 * indicator again and copies its current image.
	 */
	void sync(boolean visible) {
		dispatch(() -> {
			if (icon == null)
				return;
			var ind = indicatorPointer();
			if (ind != null)
				indicator.app_indicator_set_status(ind, APP_INDICATOR_STATUS_PASSIVE);
			var img = imageFile();
			if (img != null)
				xapp.xapp_status_icon_set_icon_name(icon, img.getAbsolutePath());
			xapp.xapp_status_icon_set_visible(icon, visible ? 1 : 0);
		});
	}

	void close() {
		dispatch(() -> {
			if (icon != null) {
				xapp.xapp_status_icon_set_visible(icon, 0);
				glib.g_object_unref(icon);
				icon = null;
			}
		});
	}

	/* dorkbox internals (SystemTray 4.1, _AppIndicatorNativeTray) */

	private Object nativeTray() throws ReflectiveOperationException {
		Field f = SystemTray.class.getDeclaredField("menu");
		f.setAccessible(true);
		return f.get(systemTray);
	}

	private Pointer indicatorPointer() {
		try {
			var tray = nativeTray();
			Field f = tray.getClass().getDeclaredField("appIndicator");
			f.setAccessible(true);
			var s = (Structure) f.get(tray);
			return s == null ? null : s.getPointer();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Could not find AppIndicator.", e);
			return null;
		}
	}

	private File imageFile() {
		try {
			var tray = nativeTray();
			Field f = tray.getClass().getDeclaredField("imageFile");
			f.setAccessible(true);
			return (File) f.get(tray);
		} catch (Exception e) {
			return null;
		}
	}

	private static void dispatch(Runnable r) {
		try {
			Class<?> c = Class.forName("dorkbox.jna.linux.GtkEventDispatch");
			Method m = c.getMethod("dispatch", Runnable.class);
			m.invoke(null, r);
		} catch (Exception e) {
			throw new IllegalStateException("No GTK dispatch thread.", e);
		}
	}
}
