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
import org.freedesktop.dbus.interfaces.DBusInterface;


/**
 * DBUS Interface.
 */
@DBusInterfaceName("org.ayatana.bamf.control")
public interface Control extends DBusInterface {

	/**
	 * Creates the local desktop file.
	 *
	 * @param application the application
	 */
	void CreateLocalDesktopFile(String application);

	/**
	 * Insert desktop file.
	 *
	 * @param desktopPath the desktop path
	 */
	void InsertDesktopFile(String desktopPath);

	/**
	 * Om nom nom desktop file.
	 *
	 * @param desktopPath the desktop path
	 */
	void OmNomNomDesktopFile(String desktopPath);

	/**
	 * Quit.
	 */
	void Quit();

	/**
	 * Register application for pid.
	 *
	 * @param application the application
	 * @param pid the pid
	 */
	void RegisterApplicationForPid(String application, int pid);

}