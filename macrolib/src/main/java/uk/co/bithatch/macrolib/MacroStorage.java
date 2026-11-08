package uk.co.bithatch.macrolib;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

/**
 * The Interface MacroStorage.
 */
public interface MacroStorage extends Closeable {
	
	/**
	 * Initialise.
	 * 
	 * @param system macro system
	 */
	void init(MacroSystem system);
	
	/**
	 * Gets the configuration.
	 *
	 * @return the configuration
	 */
	Path getConfiguration();

	/**
	 * Get the icon for the activeProfiles. This will either be a specific icon
	 * path, or if none is available, the default activeProfiles icon. If the icon
	 * is a themed icon name, then that icon will be searched for and the full path
	 * returned
	 *
	 * @param profile the profile
	 * @param height  preferred height
	 * @return icon path
	 */
	Path getProfileIconPath(MacroProfile profile, int height);

	/**
	 * Gets the profile with name.
	 *
	 * @param device the device
	 * @param name   the name
	 * @return the profile with name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	default MacroProfile getProfileWithName(MacroDevice device, String name) throws IOException {
		for (Iterator<MacroProfile> it = profiles(device); it.hasNext();) {
			MacroProfile p = it.next();
			if (name.equals(p.getName()))
				return p;
		}
		return null;
	}

	/**
	 * Get the full path of a resource (i.e. a path relative to the location of the
	 * activeProfiles's file. None will be returned if no such resource exists
	 *
	 * @param profile      the profile
	 * @param resourceName resource name
	 * @return path to resource
	 */
	Path getResourcePath(MacroProfile profile, String resourceName);

	/**
	 * Checks if is locked.
	 *
	 * @param device the device
	 * @return true, if is locked
	 */
	boolean isLocked(MacroDevice device);

	/**
	 * Load active bank.
	 *
	 * @param device the device
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	int loadActiveBank(MacroProfile device) throws IOException;

	/**
	 * Load active profile.
	 *
	 * @param device the device
	 * @return the uuid
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	UUID loadActiveProfile(MacroDevice device) throws IOException;

	/**
	 * Load default bank.
	 *
	 * @param defaultProfile the default profile
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	int loadDefaultBank(MacroProfile defaultProfile) throws IOException;

	/**
	 * Load default profile.
	 *
	 * @param device the device
	 * @return the uuid
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	UUID loadDefaultProfile(MacroDevice device) throws IOException;

	/**
	 * Load profile.
	 *
	 * @param device the device
	 * @param id     the id
	 * @return the macro profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	MacroProfile loadProfile(MacroDevice device, UUID id) throws IOException;

	/**
	 * Profiles.
	 *
	 * @param device the device
	 * @return the iterator
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	Iterator<MacroProfile> profiles(MacroDevice device) throws IOException;

	/**
	 * Save profile.
	 *
	 * @param profile the profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void saveProfile(MacroProfile profile) throws IOException;

	/**
	 * Sets the active bank.
	 *
	 * @param bank the new active bank
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void setActiveBank(MacroBank bank) throws IOException;

	/**
	 * Sets the active profile.
	 *
	 * @param profile the new active profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void setActiveProfile(MacroProfile profile) throws IOException;

	/**
	 * Sets the default bank.
	 *
	 * @param profile the profile
	 * @param bank    the bank
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void setDefaultBank(MacroProfile profile, int bank) throws IOException;

	/**
	 * Sets the default profile.
	 *
	 * @param profile the new default profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void setDefaultProfile(MacroProfile profile) throws IOException;

	/**
	 * Sets the locked.
	 *
	 * @param device the device
	 * @param locked the locked
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void setLocked(MacroDevice device, boolean locked) throws IOException;

	/**
	 * Get the number of profiles a device has.
	 * 
	 * @param device device
	 * @return number of profiles
	 * @throws IOException
	 */
	int getNumberOfProfiles(MacroDevice device) throws IOException;

	/**
	 * Remove a profile given it's ID. The default profile cannot be removed. If the
	 * profile is active and removed, another profile will then report as active (possibly the default).
	 * 
	 * @param profile profile
	 */
	void removeProfile(MacroProfile profile) throws IOException;

}
