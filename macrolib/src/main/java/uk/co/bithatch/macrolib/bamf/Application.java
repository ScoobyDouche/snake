/**
 * Java Window Monitor
 * Copyright © 2020 Bithatch (tanktarta@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.bithatch.macrolib.bamf;

import java.util.List;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;


/**
 * DBUS Interface.
 */
@DBusInterfaceName("org.ayatana.bamf.application")
public interface Application extends DBusInterface {

	/**
	 * The Class DesktopFileUpdated.
	 */
	public static class DesktopFileUpdated extends DBusSignal {

		private final String desktopFile;

		DesktopFileUpdated(String _path, String _interfaceName, String _desktopFile) throws DBusException {
			super(_path, _interfaceName);
			this.desktopFile = _desktopFile;
		}

		/**
		 * Gets the desktop file.
		 *
		 * @return the desktop file
		 */
		public String getDesktopFile() {
			return desktopFile;
		}

	}

	/**
	 * The Class SupportedMimeTypesChanged.
	 */
	public static class SupportedMimeTypesChanged extends DBusSignal {

		private final List<String> dndMimes;

		SupportedMimeTypesChanged(String _path, String _interfaceName, List<String> _dndMimes) throws DBusException {
			super(_path, _interfaceName);
			this.dndMimes = _dndMimes;
		}

		/**
		 * Gets the dnd mimes.
		 *
		 * @return the dnd mimes
		 */
		public List<String> getDndMimes() {
			return dndMimes;
		}

	}

	/**
	 * The Class WindowAdded.
	 */
	public static class WindowAdded extends DBusSignal {

		private final String viewPath;

		WindowAdded(String path, String viewPath) throws DBusException {
			super(path, viewPath);
			this.viewPath = viewPath;
		}

		/**
		 * Gets the path.
		 *
		 * @return the path
		 */
		@Override
		public String getPath() {
			return viewPath;
		}

	}

	/**
	 * The Class WindowRemoved.
	 */
	public static class WindowRemoved extends DBusSignal {

		private final String viewPath;

		WindowRemoved(String path, String viewPath) throws DBusException {
			super(path, viewPath);
			this.viewPath = viewPath;
		}

		/**
		 * Gets the path.
		 *
		 * @return the path
		 */
		@Override
		public String getPath() {
			return viewPath;
		}

	}

	/**
	 * Application menu.
	 *
	 * @return the string
	 */
	String ApplicationMenu();

	/**
	 * Application type.
	 *
	 * @return the string
	 */
	String ApplicationType();

	/**
	 * Desktop file.
	 *
	 * @return the string
	 */
	String DesktopFile();

	/**
	 * Focusable child.
	 *
	 * @return the string
	 */
	String FocusableChild();

	/**
	 * Show stubs.
	 *
	 * @return true, if successful
	 */
	boolean ShowStubs();

	/**
	 * Supported mime types.
	 *
	 * @return the list
	 */
	List<String> SupportedMimeTypes();

	/**
	 * Xids.
	 *
	 * @return the list
	 */
	List<org.freedesktop.dbus.types.UInt32> Xids();
}