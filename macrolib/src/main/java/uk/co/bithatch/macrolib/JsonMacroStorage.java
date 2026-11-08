package uk.co.bithatch.macrolib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import uk.co.bithatch.linuxio.EventCode;

/**
 * The Class JsonMacroStorage.
 */
public class JsonMacroStorage implements MacroStorage {

	private Path configuration;
	private MacroSystem system;

	/**
	 * Instantiates a new json macro storage.
	 */
	public JsonMacroStorage() {
		this(Paths.get(System.getProperty("user.home") + File.separator + ".config" + File.separator + "macrolib"));
	}

	/**
	 * Instantiates a new json macro storage.
	 */
	public JsonMacroStorage(Path configuration) {
		setConfiguration(configuration);

	}

	@Override
	public void close() throws IOException {
	}

	/**
	 * Save this activeProfiles in a format that may be transmitted to another
	 * computer (as a zip file). All references to external images (for icon and
	 * background) are made relative and added to the archive.
	 *
	 * @param profile the profile
	 * @param out     file to save copy to
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void export(MacroProfile profile, Path out) throws IOException {
		checkInit();
		profile = new MacroProfile(UUID.randomUUID(), profile);
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
			/* Icon */
			Path iconPath = profile.getIcon() != null && profile.getIcon().length() > 0 ? Paths.get(profile.getIcon())
					: null;
			if (iconPath != null && Files.exists(iconPath)) {
				String basePath = String.format("%s.resources/%s", getProfileFileName(profile.getId()),
						iconPath.getFileName().toString());
				ZipEntry zen = new ZipEntry(basePath);
				zos.putNextEntry(zen);
				try (InputStream fin = Files.newInputStream(iconPath)) {
					fin.transferTo(zos);
				}
				zos.flush();
				profile.setIcon(basePath);
			}

			/* Background */
			Path backgroundPath = profile.getBackground() != null && profile.getBackground().length() > 0
					? Paths.get(profile.getBackground())
					: null;
			if (backgroundPath != null && Files.exists(backgroundPath)) {
				String basePath = String.format("%s.resources/%s", getProfileFileName(profile.getId()),
						backgroundPath.getFileName().toString());
				ZipEntry zen = new ZipEntry(basePath);
				zos.putNextEntry(zen);
				try (InputStream fin = Files.newInputStream(iconPath)) {
					fin.transferTo(zos);
				}
				zos.flush();
				profile.setBackground(basePath);
			}

			ZipEntry zen = new ZipEntry(getProfileFileName(profile.getId()));
			zos.putNextEntry(zen);
			save(profile, zos);

			zos.flush();
		}
	}

	/**
	 * Gets the configuration.
	 *
	 * @return the configuration
	 */
	@Override
	public Path getConfiguration() {
		return configuration;
	}

	@Override
	public int getNumberOfProfiles(MacroDevice device) throws IOException {
		DirectoryStream<Path> stream = getStream(device);
		int c = 0;
		for (Iterator<Path> it = stream.iterator(); it.hasNext() && it.next() != null; c++)
			;
		return c;
	}

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
	@Override
	public Path getProfileIconPath(MacroProfile profile, int height) {
		checkInit();
		String icon = profile.getIcon();
		Path path = getResourcePath(profile, icon);
		if (path == null) {
			try {
				if (icon == null || icon.equals("")) {
					for (String s : new String[] { "preferences-desktop-keyboard-shortcuts",
							"preferences-desktop-keyboard" }) {
						if (profile.getSystem().getIconService().isIconExists(s, height))
							return profile.getSystem().getIconService().findIcon(icon, height);
					}
					throw new FileNotFoundException("No icon for activeProfiles.");
				} else
					return profile.getSystem().getIconService().findIcon(icon, height);
			} catch (IOException ioe) {
				throw new IllegalStateException("No icon for activeProfiles.", ioe);
			}
		}

		return path;
	}

	/**
	 * Gets the resource path.
	 *
	 * @param profile      the profile
	 * @param resourceName the resource name
	 * @return the resource path
	 */
	@Override
	public Path getResourcePath(MacroProfile profile, String resourceName) {
		checkInit();
		if (resourceName != null && !resourceName.equals("")) {
			if (resourceName.startsWith("/")) {
				return Paths.get(resourceName);
			}
			Path filePath = getProfileFile(profile.getDevice(), profile.getId());
			if (filePath != null) {
				Path path = filePath.resolve(resourceName);
				if (Files.exists(path))
					return path;
			}
		}
		return null;
	}

	@Override
	public void init(MacroSystem system) {
		this.system = system;
	}

	/**
	 * Checks if is locked.
	 *
	 * @param device the device
	 * @return true, if is locked
	 */
	@Override
	public boolean isLocked(MacroDevice device) {
		checkInit();
		return Files.exists(getLockFile(device));
	}

	/**
	 * Load active bank.
	 *
	 * @param profile the profile
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public int loadActiveBank(MacroProfile profile) throws IOException {
		checkInit();
		Path activeBankFile = getActiveBankFile(profile);
		int bank = 0;
		if (Files.exists(activeBankFile)) {
			try (BufferedReader r = Files.newBufferedReader(activeBankFile)) {
				bank = Integer.parseInt(r.readLine());
			}
			return bank;
		} else
			return loadDefaultBank(profile);
	}

	/**
	 * Load active profile.
	 *
	 * @param device the device
	 * @return the uuid
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public UUID loadActiveProfile(MacroDevice device) throws IOException {
		checkInit();
		Path activeProfileFile = getActiveProfileFile(device);
		if (Files.exists(activeProfileFile)) {
			try (BufferedReader r = Files.newBufferedReader(activeProfileFile)) {
				return UUID.fromString(r.readLine().trim());
			}
		}
		return null;
	}

	/**
	 * Load default bank.
	 *
	 * @param profile the profile
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public int loadDefaultBank(MacroProfile profile) throws IOException {
		checkInit();
		Path defaultProfileFile = getDefaultBankFile(profile);
		if (Files.exists(defaultProfileFile)) {
			try (BufferedReader r = Files.newBufferedReader(defaultProfileFile)) {
				return Integer.parseInt(r.readLine().trim());
			}
		}
		return 0;
	}

	/**
	 * Load default profile.
	 *
	 * @param device the device
	 * @return the uuid
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public UUID loadDefaultProfile(MacroDevice device) throws IOException {
		checkInit();
		Path defaultProfileFile = getDefaultProfileFile(device);
		if (Files.exists(defaultProfileFile)) {
			try (BufferedReader r = Files.newBufferedReader(defaultProfileFile)) {
				return UUID.fromString(r.readLine().trim());
			}
		} else {
			Iterator<MacroProfile> it = profiles(device);
			if (it.hasNext()) {
				MacroProfile profile = it.next();
				UUID id = profile.getId();
				setDefaultProfile(profile);
				return id;
			}
		}
		return null;
	}

	/**
	 * Load profile.
	 *
	 * @param device the device
	 * @param id     the id
	 * @return the macro profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public MacroProfile loadProfile(MacroDevice device, UUID id) throws IOException {
		checkInit();
		Path activeProfileFile = getProfileFile(device, id);
		if (!Files.exists(activeProfileFile))
			return null;
		else {
			return loadProfile(device, activeProfileFile);
		}
	}

	/**
	 * Profiles.
	 *
	 * @param device the device
	 * @return the iterator
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public Iterator<MacroProfile> profiles(MacroDevice device) throws IOException {
		checkInit();
		DirectoryStream<Path> stream = getStream(device);
		Iterator<Path> it = stream.iterator();
		return new Iterator<MacroProfile>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public MacroProfile next() {
				try {
					return loadProfile(device, it.next());
				} catch (IOException e) {
					throw new IllegalStateException("Failed to iterate profiles.", e);
				} finally {
					if (!it.hasNext()) {
						try {
							stream.close();
						} catch (IOException e) {
						}
					}
				}
			}
		};
	}

	@Override
	public void removeProfile(MacroProfile profile) throws IOException {
		MacroDevice device = profile.getDevice();
		UUID id = profile.getId();
		UUID defId = loadDefaultProfile(device);
		if (id.equals(defId)) {
			throw new IllegalStateException(
					"Cannot remove default profile. Make another profile the default before deleting this one.");
		}
		UUID actId = loadActiveProfile(device);
		if (id.equals(actId)) {
			Files.delete(getProfileFile(device, id));
		}
		Files.delete(getActiveBankFile(profile));
		Files.delete(getDefaultBankFile(profile));
	}

	/**
	 * Save profile.
	 *
	 * @param profile the profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void saveProfile(MacroProfile profile) throws IOException {
		checkInit();

		Path tmpFile = getProfiles(profile.getDevice()).resolve(getProfileFileName(profile.getId()) + ".tmp");
		try (OutputStream out = Files.newOutputStream(tmpFile)) {
			save(profile, out);
		}

		/* Check we can load the new one (don't do anything with it though) */
		loadProfile(profile.getDevice(), tmpFile);

		/* All good, rename */
		Path activeProfileFile = getProfileFile(profile.getDevice(), profile.getId());
		Files.move(tmpFile, activeProfileFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	/**
	 * Sets the active bank.
	 *
	 * @param bank the new active bank
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void setActiveBank(MacroBank bank) throws IOException {
		checkInit();
		Path activeBankFile = getActiveBankFile(bank.getProfile());
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(activeBankFile), true)) {
			pw.println(String.valueOf(bank.getBank()));
		}

	}

	/**
	 * Sets the active profile.
	 *
	 * @param profile the new active profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void setActiveProfile(MacroProfile profile) throws IOException {
		Path activeProfileFile = getActiveProfileFile(profile.getDevice());
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(activeProfileFile), true)) {
			pw.println(profile.getId());
		}
	}

	/**
	 * Sets the configuration.
	 *
	 * @param configuration the new configuration
	 */
	public void setConfiguration(Path configuration) {
		if (!Files.exists(configuration)) {
			try {
				Files.createDirectories(configuration);
			} catch (IOException e) {
				throw new IllegalArgumentException(String.format("Could not create directory %s.", configuration));
			}
		}
		this.configuration = configuration;
	}

	/**
	 * Sets the default bank.
	 *
	 * @param profile the profile
	 * @param bank    the bank
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void setDefaultBank(MacroProfile profile, int bank) throws IOException {
		checkInit();
		Path activeBankFile = getActiveBankFile(profile);
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(activeBankFile), true)) {
			pw.println(String.valueOf(bank));
		}

	}

	/**
	 * Sets the default profile.
	 *
	 * @param profile the new default profile
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void setDefaultProfile(MacroProfile profile) throws IOException {
		checkInit();
		Path defaultProfileFile = getDefaultProfileFile(profile.getDevice());
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(defaultProfileFile), true)) {
			pw.println(profile.getId());
		}
	}

	/**
	 * Sets the locked.
	 *
	 * @param device the device
	 * @param locked the locked
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void setLocked(MacroDevice device, boolean locked) throws IOException {
		checkInit();
		Path p = getLockFile(device);
		if (locked && !Files.exists(p)) {
			Files.createFile(p);
		} else if (!locked && Files.exists(p)) {
			Files.delete(p);
		}

	}

	protected Path checkDir(Path dir) {
		if (!Files.exists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to create activeProfiles directory.", e);
			}
		}
		return dir;
	}

	protected void checkInit() {
		if (system == null)
			throw new IllegalStateException("Not initialised.");
	}

	protected Path getActiveBankFile(MacroProfile profile) {
		return getProfiles(profile.getDevice()).resolve(String.format("%s.activeBank", profile.getId()));
	}

	protected String getActiveBankFileName(UUID id) {
		return String.format("%s.active", id);
	}

	protected Path getActiveProfileFile(MacroDevice device) {
		return getDevice(device).resolve("active");
	}

	protected Path getDefaultBankFile(MacroProfile profile) {
		return getProfiles(profile.getDevice()).resolve(String.format("%s.defaultBank", profile.getId()));
	}

	protected String getDefaultBankFileName(UUID id) {
		return String.format("%s.default", id);
	}

	protected Path getDefaultProfileFile(MacroDevice device) {
		return getDevice(device).resolve("default");
	}

	protected Path getDevice(MacroDevice device) {
		return checkDir(getConfiguration().resolve(device.getUID()));
	}

	protected Path getLockFile(MacroDevice device) {
		return getDevice(device).resolve(String.format("lock", device.getUID()));
	}

	protected Path getProfileFile(MacroDevice device, UUID id) {
		return getProfiles(device).resolve(getProfileFileName(id));
	}

	protected String getProfileFileName(UUID id) {
		return String.format("%s.json", id);
	}

	protected Path getProfiles(MacroDevice device) {
		return checkDir(getDevice(device).resolve("profiles"));
	}

	protected DirectoryStream<Path> getStream(MacroDevice device) throws IOException {
		DirectoryStream<Path> stream = Files.newDirectoryStream(getProfiles(device),
				(f) -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"));
		return stream;
	}

	protected MacroProfile loadProfile(MacroDevice device, Path activeProfileFile) throws IOException {
		GsonBuilder gson = createGsonBuilder();
		try (Reader reader = Files.newBufferedReader(activeProfileFile)) {
			MacroProfile profile = gson.create().fromJson(reader, MacroProfile.class);
			if (profile == null)
				throw new IOException(String.format("Could not parse JSON file %s.", activeProfileFile));
			profile.setDevice(device);
			profile.setSystem(system);
			for (MacroBank bank : profile.getBanks()) {
				bank.setProfile(profile);
			}
			return profile;
		}
	}

	protected GsonBuilder createGsonBuilder() {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(new TypeToken<MacroBank>() {
		}.getType(), new JsonDeserializer<MacroBank>() {

			@Override
			public MacroBank deserialize(JsonElement src, Type typeOfSrc, JsonDeserializationContext context) {
				MacroBank bank = new MacroBank();
				JsonObject bankObject = src.getAsJsonObject();
				bank.setBank(bankObject.get("bank").getAsInt());
				bank.setName(bankObject.has("name") ? bankObject.get("name").getAsString() : null);
				Gson parser = gson.create();
				if (bankObject.has("properties")) {
					JsonObject properties = bankObject.get("properties").getAsJsonObject();
					for (String k : properties.keySet()) {
						bank.getProperties().put(k, parser.fromJson(properties.get(k), Object.class));
					}
				}
				for (JsonElement keyEl : bankObject.get("macros").getAsJsonArray()) {
					bank.add(parser.fromJson(keyEl.getAsJsonObject(), Macro.class));
				}
				return bank;
			}
		});
		gson.registerTypeAdapter(new TypeToken<KeySequence>() {
		}.getType(), new JsonDeserializer<KeySequence>() {
			@Override
			public KeySequence deserialize(JsonElement src, Type typeOfSrc, JsonDeserializationContext context) {
				KeySequence seq = new KeySequence(KeyState.valueOf(src.getAsJsonObject().get("state").getAsString()));
				for (JsonElement keyEl : src.getAsJsonObject().get("keys").getAsJsonArray()) {
					seq.add(EventCode.valueOf(keyEl.getAsString()));
				}
				return seq;
			}
		});

		gson.registerTypeAdapter(new TypeToken<Macro>() {
		}.getType(), new JsonDeserializer<Macro>() {
			@Override
			public Macro deserialize(JsonElement src, Type typeOfSrc, JsonDeserializationContext context) {
				JsonObject macroObj = src.getAsJsonObject();
				TargetType targetType = TargetType.valueOf(macroObj.get("type").getAsString());
				switch (targetType) {
				case ACTION:
					return gson.create().fromJson(macroObj, ActionMacro.class);
				case COMMAND:
					return gson.create().fromJson(macroObj, CommandMacro.class);
				case DIGITAL_JOYSTICK:
				case JOYSTICK:
				case KEYBOARD:
				case MOUSE:
					UInputMacro uinput = gson.create().fromJson(macroObj, UInputMacro.class);
					uinput.setType(targetType);
					return uinput;
				case SIMPLE:
					return gson.create().fromJson(macroObj, SimpleMacro.class);
				case SCRIPT:
					return gson.create().fromJson(macroObj, ScriptMacro.class);
				default:
					return gson.create().fromJson(macroObj, NoopMacro.class);
				}
			}
		});
		return gson;
	}

	protected void save(MacroProfile profile, OutputStream os) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(new TypeToken<KeySequence>() {
		}.getType(), new JsonSerializer<KeySequence>() {
			@Override
			public JsonElement serialize(KeySequence src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject root = new JsonObject();
				root.addProperty("state", src.getState().name());
				JsonArray keys = new JsonArray();
				for (EventCode code : src)
					keys.add(code.name());
				root.add("keys", keys);
				return root;
			}
		});
		gson.setPrettyPrinting();
		Gson parser = gson.create();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
		pw.println(parser.toJson(profile));
		pw.flush();
	}

}
