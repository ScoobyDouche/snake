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

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;


/**
 * DBUS Interface.
 */
@DBusInterfaceName("org.ayatana.bamf.window")
public interface Window extends DBusInterface {

	/**
	 * The Class MaximizedChanged.
	 */
	public static class MaximizedChanged extends DBusSignal {

		private final int newValue;
		private final int oldValue;

		MaximizedChanged(String _path, String _interfaceName, int oldValue, int newValue) throws DBusException {
			super(_path, _interfaceName);
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		/**
		 * Gets the new value.
		 *
		 * @return the new value
		 */
		public int getNewValue() {
			return newValue;
		}

		/**
		 * Gets the old value.
		 *
		 * @return the old value
		 */
		public int getOldValue() {
			return oldValue;
		}

	}

	/**
	 * The Class MonitorChanged.
	 */
	public static class MonitorChanged extends DBusSignal {

		private final int newMonitor;
		private final int oldMonitor;

		MonitorChanged(String path, int oldMonitor, int newMonitor) throws DBusException {
			super(path, oldMonitor, newMonitor);
			this.oldMonitor = oldMonitor;
			this.newMonitor = newMonitor;
		}

		/**
		 * Gets the new monitor.
		 *
		 * @return the new monitor
		 */
		public int getNewMonitor() {
			return newMonitor;
		}

		/**
		 * Gets the old monitor.
		 *
		 * @return the old monitor
		 */
		public int getOldMonitor() {
			return oldMonitor;
		}

	}

	/**
	 * Gets the pid.
	 *
	 * @return the u int 32
	 */
	UInt32 GetPid();

	/**
	 * Gets the xid.
	 *
	 * @return the u int 32
	 */
	UInt32 GetXid();

	/**
	 * Maximized.
	 *
	 * @return the int
	 */
	int Maximized();

	/**
	 * Monitor.
	 *
	 * @return the int
	 */
	int Monitor();

	/**
	 * Transient.
	 *
	 * @return the string
	 */
	String Transient();

	/**
	 * Window type.
	 *
	 * @return the u int 32
	 */
	UInt32 WindowType();

	/**
	 * Xprop.
	 *
	 * @param xprop the xprop
	 * @return the string
	 */
	String Xprop(String xprop);
}