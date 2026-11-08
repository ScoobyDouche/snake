package uk.co.bithatch.macrolib;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import uk.co.bithatch.linuxio.EventCode;

/**
 * Encapsulates a single macro activeProfiles with a number memory banks. This
 * object contains all the general information about the active profiles, as
 * well as the list of macros themselves.
 */
public class MacroProfile {

	/** The Constant DEFAULT_REPEAT_DELAY. */
	public final static double DEFAULT_REPEAT_DELAY = -1.0;

	static <O extends Comparable<O>> List<O> sorted(Collection<O> o) {
		List<O> l = new ArrayList<>(o);
		Collections.sort(l);
		return l;
	}

	private String author;
	private String background;
	private transient MacroProfile baseProfile;
	private transient MacroDevice device;
	private Set<String> excludeApplications = new LinkedHashSet<>();
	private boolean fixedDelays;
	private String icon;
	private UUID id;
	private Set<String> includeApplications = new LinkedHashSet<>();
	private Map<Integer, MacroBank> macros = new HashMap<>();
	private List<String> models = new ArrayList<>();
	private String name;
	private long pressDelay;
	private Map<String, Object> properties = new HashMap<>();
	private boolean readOnly;
	private long releaseDelay;

	private boolean sendDelays;
	private transient MacroSystem system;
	private float version;

	MacroProfile() {
	}

	/**
	 * Constructor
	 *
	 * @param system    macro system
	 * @param device    device
	 * @param profileId activeProfiles id
	 */
	MacroProfile(MacroSystem system, MacroDevice device, UUID profileId) {
		this.system = system;
		this.device = device;
		this.readOnly = false;
		this.icon = null;
		this.background = null;
		this.id = null;
		if (profileId != null) {
			setId(profileId);
		}
		this.author = "";
		this.macros = Collections.synchronizedMap(new LinkedHashMap<>());
		this.models = Arrays.asList(device.getId());
		this.baseProfile = null;
		this.version = 1.0f;
	}

	/**
	 * Constructor
	 *
	 * @param system    macro system
	 * @param profileId activeProfiles id
	 */
	MacroProfile(MacroSystem system, UUID profileId) {
		this(system, null, profileId);
	}

	MacroProfile(UUID id, MacroProfile profile) {
		this.author = profile.getAuthor();
		this.name = profile.name;
		this.background = profile.getBackground();
		this.baseProfile = profile.getBaseProfile();
		this.properties.putAll(profile.getProperties());
		this.device = profile.getDevice();
		this.fixedDelays = profile.isFixedDelays();
		this.icon = profile.getIcon();
		this.id = id;
		this.macros = new HashMap<>();
		for (MacroBank bank : profile.getMacros().values()) {
			this.macros.put(bank.getBank(), new MacroBank(this, bank));
		}
		this.models = new ArrayList<>(profile.getModels());
		this.pressDelay = profile.getPressDelay();
		this.readOnly = profile.isReadOnly();
		this.releaseDelay = profile.getReleaseDelay();
		this.sendDelays = profile.isSendDelays();
		this.system = profile.getSystem();
		this.version = profile.getVersion();
		this.includeApplications.addAll(profile.includeApplications);
		this.excludeApplications.addAll(profile.excludeApplications);
	}

	/**
	 * Adds the bank.
	 *
	 * @param name the name
	 * @return the macro bank
	 */
	public MacroBank createBank(String name) {
		MacroBank bank = getBank(nextFreeBankNumber());
		bank.setName(name);
		getSystem().bankAdded(bank);
		return bank;
	}

	/**
	 * Adds the bank.
	 *
	 * @param name the name
	 * @return the macro bank
	 */
	MacroBank addBank(String name) {
		MacroBank bank = getBank(nextFreeBankNumber());
		bank.setName(name);
		return bank;
	}

	/**
	 * Get if the specified keys are currently in use for a macro in the supplied
	 * memory bank number. Optionally, a list of macros that should be excluded from
	 * the search can be supplied (usually used to exclude the current macro when
	 * checking if other macros currently use a set of keys)
	 *
	 * @param activate_on the key state to activate the macro on
	 * @param memory      memory bank number
	 * @param keys        keys to search for
	 * @param exclude     list of macro objects to exclude
	 * @return true, if successful
	 */
	public boolean areKeysInUse(KeyState activate_on, int memory, List<EventCode> keys, List<Macro> exclude) {
		Set<Macro> bank = doGetMacros(activate_on, memory, true);
		for (Macro macro : bank) {
			if ((exclude == null || (exclude != null && !isExcluded(exclude, macro)))
					&& sorted(keys) == sorted(macro.getActivatedBy())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacroProfile other = (MacroProfile) obj;
		if (device == null) {
			if (other.device != null)
				return false;
		} else if (!device.equals(other.device))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
	 * Gets the author.
	 *
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Gets the background.
	 *
	 * @return the background
	 */
	public String getBackground() {
		return background;
	}

	/**
	 * Gets the bank.
	 *
	 * @param bank the bank
	 * @return the bank
	 */
	public MacroBank getBank(int bank) {
		return doGetBank(bank, true);
	}

	/**
	 * Gets the bank.
	 *
	 * @param name the name
	 * @return the bank
	 */
	public MacroBank getBank(String name) {
		for (MacroBank bank : macros.values()) {
			if (name.equals(bank.getName())) {
				return bank;
			}
		}
		return null;
	}

	/**
	 * Gets the banks.
	 *
	 * @return the banks
	 */
	public Collection<MacroBank> getBanks() {
		return macros.values();
	}

	/**
	 * Gets the base profile.
	 *
	 * @return the base profile
	 */
	public MacroProfile getBaseProfile() {
		return baseProfile;
	}

	/**
	 * Get an ActionBinding if this activeProfiles contains a map to the supplied
	 * action name.
	 *
	 * @param activateOn the key state to activate the macro on
	 * @param actionName name of action
	 * @return the binding for action
	 */
	public ActionBinding getBindingForAction(KeyState activateOn, String actionName) {
		for (Map.Entry<Integer, MacroBank> bank : macros.entrySet()) {
			for (Macro m : doGetMacros(activateOn, bank.getKey(), true)) {
				if (m.getType() == TargetType.ACTION && ((ActionMacro) m).getAction().equals(actionName)
						&& activateOn == m.getState()) {
					// TODO held actions?
					return new ActionBinding(getDevice(), actionName, m.getActivatedBy());
				}
			}
		}
		return null;
	}

	/**
	 * Gets the device.
	 *
	 * @return the device
	 */
	public MacroDevice getDevice() {
		return device;
	}

	/**
	 * Gets the exclude applications.
	 *
	 * @return the exclude applications
	 */
	public Set<String> getExcludeApplications() {
		return excludeApplications;
	}

	/**
	 * Gets the icon.
	 *
	 * @return the icon
	 */
	public String getIcon() {
		return icon;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Gets the include applications.
	 *
	 * @return the include applications
	 */
	public Set<String> getIncludeApplications() {
		return includeApplications;
	}

	/**
	 * Get the macro given the memory bank number and the list of keys the macro
	 * requires to activate.
	 *
	 * @param activatedBy the activated by
	 * @param memory      memory bank number (starts at 1)
	 * @return the macro
	 */
	public Macro getMacro(KeySequence activatedBy, int memory) {
		MacroBank bank = macros.get(memory);
		if (bank != null) {
			for (Macro macro : bank.getMacros()) {
				int keyCount = 0;
				for (EventCode k : macro.getActivatedBy()) {
					if (activatedBy.contains(k))
						keyCount += 1;
				}
				if (keyCount == macro.getActivatedBy().size() && keyCount == activatedBy.size())
					return macro;
			}
		}
		return null;
	}

	/**
	 * Gets the macros.
	 *
	 * @return the macros
	 */
	public Map<Integer, MacroBank> getMacros() {
		return Collections.unmodifiableMap(macros);
	}

	/**
	 * Gets the macros.
	 *
	 * @param keyState the key state
	 * @param bank     the bank
	 * @return the macros
	 */
	public Set<Macro> getMacros(KeyState keyState, int bank) {
		return Collections.unmodifiableSet(doGetMacros(keyState, bank, true));
	}

	/**
	 * Gets the models.
	 *
	 * @return the models
	 */
	public List<String> getModels() {
		return models;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the press delay.
	 *
	 * @return the press delay
	 */
	public long getPressDelay() {
		return pressDelay;
	}

	/**
	 * Get the icon for the activeProfiles. This will either be a specific icon
	 * path, or if none is available, the default activeProfiles icon. If the icon
	 * is a themed icon name, then that icon will be searched for and the full path
	 * returned
	 * 
	 * @param height preferred height
	 * @return icon path
	 */
	public Path getProfileIconPath(int height) {
		return system.getWriter().getProfileIconPath(this, height);
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Gets the release delay.
	 *
	 * @return the release delay
	 */
	public long getReleaseDelay() {
		return releaseDelay;
	}

	/**
	 * Get the full path of a resource (i.e. a path relative to the location of the
	 * activeProfiles's file. None will be returned if no such resource exists
	 *
	 * @param resourceName resource name
	 * @return path to resource
	 */
	public Path getResourcePath(String resourceName) {
		return system.getWriter().getResourcePath(this, resourceName);
	}

	/**
	 * Gets the system.
	 *
	 * @return the system
	 */
	public MacroSystem getSystem() {
		return system;
	}

	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public float getVersion() {
		return version;
	}

	/**
	 * Checks for free banks.
	 *
	 * @return true, if successful
	 */
	public boolean hasFreeBanks() {
		try {
			nextFreeBankNumber();
			return true;
		} catch (IllegalStateException ise) {
			return false;
		}
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * Checks if is activated by application.
	 *
	 * @return true, if is activated by application
	 */
	public boolean isActivatedByApplication() {
		return !includeApplications.isEmpty() || !excludeApplications.isEmpty();
	}

	/**
	 * Get if this activeProfiles is the currently active one.
	 *
	 * @return active
	 */
	public boolean isActive() {
		return this.equals(system.getActiveProfile(device));
	}

	/**
	 * Get if this activeProfiles is the default one.
	 *
	 * @return default
	 */
	public boolean isDefault() {
		return this.equals(system.getDefaultProfile(device));
	}

	/**
	 * Checks if is fixed delays.
	 *
	 * @return true, if is fixed delays
	 */
	public boolean isFixedDelays() {
		return fixedDelays;
	}

	/**
	 * Checks if is read only.
	 *
	 * @return true, if is read only
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Checks if is send delays.
	 *
	 * @return true, if is send delays
	 */
	public boolean isSendDelays() {
		return sendDelays;
	}

	/**
	 * Make this the currently active activeProfiles. An Exception will be raised if
	 * the activeProfiles is currently locked for this device
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void makeActive() throws IOException {
		system.setActiveProfile(this);
	}

	/**
	 * Next free bank number.
	 *
	 * @return the int
	 */
	public int nextFreeBankNumber() {
		for (int i = 0; i < getDevice().getBanks(); i++) {
			if (!macros.containsKey(i)) {
				return i;
			}
		}
		throw new IllegalStateException("No more free banks.");
	}

	/**
	 * Removes a bank.
	 *
	 * @param macroBank the macro bank
	 */
	public void remove(MacroBank macroBank) {
		macros.remove(macroBank.getBank());
		getSystem().removedBank(macroBank);
	}

	/**
	 * Removes this profile.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void remove() throws IOException {
		getSystem().removedProfile(this);
	}

	/**
	 * Sets the author.
	 *
	 * @param author the new author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Sets the background.
	 *
	 * @param background the new background
	 */
	public void setBackground(String background) {
		this.background = background;
	}

	/**
	 * Sets the base profile.
	 *
	 * @param baseProfile the new base profile
	 */
	public void setBaseProfile(MacroProfile baseProfile) {
		this.baseProfile = baseProfile;
	}

	/**
	 * Sets the fixed delays.
	 *
	 * @param fixedDelays the new fixed delays
	 */
	public void setFixedDelays(boolean fixedDelays) {
		this.fixedDelays = fixedDelays;
	}

	/**
	 * Sets the icon.
	 *
	 * @param icon the new icon
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Sets the models.
	 *
	 * @param models the new models
	 */
	public void setModels(List<String> models) {
		this.models = models;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the press delay.
	 *
	 * @param pressDelay the new press delay
	 */
	public void setPressDelay(long pressDelay) {
		this.pressDelay = pressDelay;
	}

	/**
	 * Sets the release delay.
	 *
	 * @param releaseDelay the new release delay
	 */
	public void setReleaseDelay(long releaseDelay) {
		this.releaseDelay = releaseDelay;
	}

	/**
	 * Sets the send delays.
	 *
	 * @param sendDelays the new send delays
	 */
	public void setSendDelays(boolean sendDelays) {
		this.sendDelays = sendDelays;
	}

	/**
	 * Sets the version.
	 *
	 * @param version the new version
	 */
	public void setVersion(float version) {
		this.version = version;
	}

	/**
	 * Make this profile the default.
	 */
	public void makeDefault() {
		getSystem().setDefaultProfile(this);
	}

	/**
	 * Save this profiles changes.
	 */
	public void commit() {
		getSystem().profileChanged(this);
	}

	protected MacroBank doGetBank(int bankNumber, boolean add) {
		synchronized (macros) {
			MacroBank bank = macros.get(bankNumber);
			if (bank == null && add) {
				bank = new MacroBank(this, bankNumber);
				macros.put(bankNumber, bank);
			}
			return bank;
		}
	}

	protected boolean isExcluded(List<Macro> exclude, Macro macro) {
		return exclude.contains(macro);
	}

	Set<Macro> doGetMacros(KeyState keyState, int bank, boolean add) {
		synchronized (macros) {
			return doGetBank(bank, add).getMacros(keyState);
		}
	}

	/**
	 * 
	 * Get the list of macros sorted
	 * 
	 * @param activateOn   the state the macro is activated on
	 * @param memoryNumber memory bank number to retrieve macros from (starts at 1)
	 */
	List<Macro> getSortedMacros(KeyState activateOn, int memoryNumber) {
		List<Macro> sm = new ArrayList<>();
		if (activateOn == null) {
			sm.addAll(getBank(memoryNumber).getMacros());
		} else {
			sm.addAll(getBank(memoryNumber).getMacros(activateOn));
		}
		Collections.sort(sm);
		return sm;
	}

	void setDevice(MacroDevice device) {
		this.device = device;
	}

	void setId(UUID id) {
		this.id = id;
		readOnly = false;
	}

	void setSystem(MacroSystem system) {
		this.system = system;
	}

}
