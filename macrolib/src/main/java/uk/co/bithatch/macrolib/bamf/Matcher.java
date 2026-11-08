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
import org.freedesktop.dbus.types.UInt32;


/**
 * DBUS Interface.
 */
@DBusInterfaceName("org.ayatana.bamf.matcher")
public interface Matcher extends DBusInterface {

	/**
	 * The Class ActiveApplicationChanged.
	 */
	public static class ActiveApplicationChanged extends DBusSignal {

		private final String newApp;
		private final String oldApp;

		/**
		 * Instantiates a new active application changed.
		 *
		 * @param path the path
		 * @param oldApp the old app
		 * @param newApp the new app
		 * @throws DBusException the d bus exception
		 */
		public ActiveApplicationChanged(String path, String oldApp, String newApp)
				throws DBusException {
			super(path, oldApp, newApp);
			this.oldApp = oldApp;
			this.newApp = newApp;
		}

		/**
		 * Gets the new app.
		 *
		 * @return the new app
		 */
		public String getNewApp() {
			return newApp;
		}

		/**
		 * Gets the old app.
		 *
		 * @return the old app
		 */
		public String getOldApp() {
			return oldApp;
		}

	}

	/**
	 * The Class ActiveWindowChanged.
	 */
	public static class ActiveWindowChanged extends DBusSignal {

		private final String newWin;
		private final String oldWin;

		/**
		 * Instantiates a new active window changed.
		 *
		 * @param path the path
		 * @param oldWin the old win
		 * @param newWin the new win
		 * @throws DBusException the d bus exception
		 */
		public ActiveWindowChanged(String path, String oldWin, String newWin) throws DBusException {
			super(path, oldWin, newWin);
			this.oldWin = oldWin;
			this.newWin = newWin;
		}

//		ActiveWindowChanged(String _path, String _interfaceName, String _oldWin, String _newWin) throws DBusException {
//			super(_path, _interfaceName);
//			this.oldWin = _oldWin;
//			this.newWin = _newWin;
//		}

		/**
		 * Gets the new win.
		 *
		 * @return the new win
		 */
		public String getNewWin() {
			return newWin;
		}

		/**
		 * Gets the old win.
		 *
		 * @return the old win
		 */
		public String getOldWin() {
			return oldWin;
		}

	}

	/**
	 * The Class RunningApplicationsChanged.
	 */
	public static class RunningApplicationsChanged extends DBusSignal {

		private final List<String> closedDesktopFiles;
		private final List<String> openedDesktopFiles;

		RunningApplicationsChanged(String _path, String _interfaceName, List<String> _openedDesktopFiles,
				List<String> _closedDesktopFiles) throws DBusException {
			super(_path, _interfaceName);
			this.openedDesktopFiles = _openedDesktopFiles;
			this.closedDesktopFiles = _closedDesktopFiles;
		}

		/**
		 * Gets the closed desktop files.
		 *
		 * @return the closed desktop files
		 */
		public List<String> getClosedDesktopFiles() {
			return closedDesktopFiles;
		}

		/**
		 * Gets the opened desktop files.
		 *
		 * @return the opened desktop files
		 */
		public List<String> getOpenedDesktopFiles() {
			return openedDesktopFiles;
		}

	}

	/**
	 * The Class ViewClosed.
	 */
	public static class ViewClosed extends DBusSignal {

		private final String viewPath;
		private final String viewType;

		ViewClosed(String path, String interfaceName, String viewPath, String viewType) throws DBusException {
			super(path, interfaceName);
			this.viewPath = viewPath;
			this.viewType = viewType;
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

		/**
		 * Gets the view type.
		 *
		 * @return the view type
		 */
		public String getViewType() {
			return viewType;
		}

	}

	/**
	 * The Class ViewOpened.
	 */
	public static class ViewOpened extends DBusSignal {

		private final String viewPath;
		private final String viewType;

		ViewOpened(String path, String interfaceName, String viewPath, String viewType) throws DBusException {
			super(path, interfaceName);
			this.viewPath = viewPath;
			this.viewType = viewType;
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

		/**
		 * Gets the view type.
		 *
		 * @return the view type
		 */
		public String getViewType() {
			return viewType;
		}

	}

	/**
	 * Active application.
	 *
	 * @return the string
	 */
	String ActiveApplication();

	/**
	 * Active window.
	 *
	 * @return the string
	 */
	String ActiveWindow();

	/**
	 * Application for xid.
	 *
	 * @param xid the xid
	 * @return the string
	 */
	String ApplicationForXid(UInt32 xid);

	/**
	 * Application is running.
	 *
	 * @param desktopFile the desktop file
	 * @return true, if successful
	 */
	boolean ApplicationIsRunning(String desktopFile);

	/**
	 * Application paths.
	 *
	 * @return the list
	 */
	List<String> ApplicationPaths();

	/**
	 * Path for application.
	 *
	 * @param desktopFile the desktop file
	 * @return the string
	 */
	String PathForApplication(String desktopFile);

	/**
	 * Register favorites.
	 *
	 * @param favorites the favorites
	 */
	void RegisterFavorites(List<String> favorites);

	/**
	 * Running applications.
	 *
	 * @return the list
	 */
	List<String> RunningApplications();

	/**
	 * Running applications desktop files.
	 *
	 * @return the list
	 */
	List<String> RunningApplicationsDesktopFiles();

	/**
	 * Tab paths.
	 *
	 * @return the list
	 */
	List<String> TabPaths();

	/**
	 * Window paths.
	 *
	 * @return the list
	 */
	List<String> WindowPaths();

	/**
	 * Window stack for monitor.
	 *
	 * @param monitorId the monitor id
	 * @return the list
	 */
	List<String> WindowStackForMonitor(int monitorId);

	/**
	 * Xids for application.
	 *
	 * @param desktopFile the desktop file
	 * @return the list
	 */
	List<UInt32> XidsForApplication(String desktopFile);
}