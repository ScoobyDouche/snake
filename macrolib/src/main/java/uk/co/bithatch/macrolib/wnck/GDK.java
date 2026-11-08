/**
 * Java WNCK bindings
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
package uk.co.bithatch.macrolib.wnck;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.PointerByReference;


/**
 * The Interface GDK.
 */
public interface GDK extends Library {

	/** The Constant INSTANCE. */
	public static final GDK INSTANCE = Native.load(GDK.JNA_LIBRARY_NAME, GDK.class);

	/** The Constant JNA_LIBRARY_NAME. */
	public static final String JNA_LIBRARY_NAME = "gdk-3";

	/** The Constant JNA_NATIVE_LIB. */
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(GDK.JNA_LIBRARY_NAME);

	/**
	 * G list free.
	 *
	 * @param list the list
	 */
	void g_list_free(PointerByReference list);

	/**
	 * Gdk init.
	 *
	 * @param argc the argc
	 * @param argv the argv
	 */
	void gdk_init(int argc, String[] argv);
}
