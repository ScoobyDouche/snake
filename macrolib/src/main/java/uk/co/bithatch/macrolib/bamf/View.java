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
@DBusInterfaceName("org.ayatana.bamf.view")
public interface View extends DBusInterface {

	/**
	 * The Class ActiveChanged.
	 */
	public static class ActiveChanged extends DBusSignal {

		private final boolean isActive;

		ActiveChanged(String _path, String _interfaceName, boolean _isActive) throws DBusException {
			super(_path, _interfaceName);
			this.isActive = _isActive;
		}

		/**
		 * Gets the checks if is active.
		 *
		 * @return the checks if is active
		 */
		public boolean getIsActive() {
			return isActive;
		}

	}

	/**
	 * The Class ChildAdded.
	 */
	public static class ChildAdded extends DBusSignal {

		private final String viewPath;

		ChildAdded(String path, String viewPath) throws DBusException {
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
	 * The Class ChildRemoved.
	 */
	public static class ChildRemoved extends DBusSignal {

		private final String viewPath;

		ChildRemoved(String path, String viewPath) throws DBusException {
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
	 * The Class NameChanged.
	 */
	public static class NameChanged extends DBusSignal {

		private final String newName;
		private final String oldName;

		NameChanged(String path, String oldName, String newName) throws DBusException {
			super(path, oldName, newName);
			this.oldName = oldName;
			this.newName = newName;
		}

		/**
		 * Gets the new name.
		 *
		 * @return the new name
		 */
		public String getNewName() {
			return newName;
		}

		/**
		 * Gets the old name.
		 *
		 * @return the old name
		 */
		public String getOldName() {
			return oldName;
		}

	}

	/**
	 * The Class RunningChanged.
	 */
	public static class RunningChanged extends DBusSignal {

		private final boolean isRunning;

		RunningChanged(String path, boolean isRunning) throws DBusException {
			super(path, isRunning);
			this.isRunning = isRunning;
		}

		/**
		 * Gets the checks if is running.
		 *
		 * @return the checks if is running
		 */
		public boolean getIsRunning() {
			return isRunning;
		}

	}

	/**
	 * The Class UrgentChanged.
	 */
	public static class UrgentChanged extends DBusSignal {

		private final boolean isUrgent;

		UrgentChanged(String path, boolean isUrgent) throws DBusException {
			super(path, isUrgent);
			this.isUrgent = isUrgent;
		}

		/**
		 * Gets the checks if is urgent.
		 *
		 * @return the checks if is urgent
		 */
		public boolean getIsUrgent() {
			return isUrgent;
		}

	}

	/**
	 * The Class UserVisibleChanged.
	 */
	public static class UserVisibleChanged extends DBusSignal {

		private final boolean userVisible;

		UserVisibleChanged(String path, boolean userVisible) throws DBusException {
			super(path, userVisible);
			this.userVisible = userVisible;
		}

		/**
		 * Gets the user visible.
		 *
		 * @return the user visible
		 */
		public boolean getUserVisible() {
			return userVisible;
		}

	}

	/**
	 * Children.
	 *
	 * @return the list
	 */
	List<String> Children();

	/**
	 * Icon.
	 *
	 * @return the string
	 */
	String Icon();

	/**
	 * Checks if is active.
	 *
	 * @return true, if successful
	 */
	boolean IsActive();

	/**
	 * Checks if is running.
	 *
	 * @return true, if successful
	 */
	boolean IsRunning();

	/**
	 * Checks if is urgent.
	 *
	 * @return true, if successful
	 */
	boolean IsUrgent();

	/**
	 * Name.
	 *
	 * @return the string
	 */
	String Name();

	/**
	 * Parents.
	 *
	 * @return the list
	 */
	List<String> Parents();

	/**
	 * User visible.
	 *
	 * @return true, if successful
	 */
	boolean UserVisible();

	/**
	 * View type.
	 *
	 * @return the string
	 */
	String ViewType();
}