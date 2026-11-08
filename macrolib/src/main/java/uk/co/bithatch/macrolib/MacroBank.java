package uk.co.bithatch.macrolib;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Type;

/**
 * The Class MacroBank.
 */
public class MacroBank {

	class MacroList extends AbstractList<Macro> {

		private transient List<Macro> delegate = new ArrayList<>();

		@Override
		public void add(int index, Macro value) {
			delegate.add(index, value);
			putMacro(value);
			rebuild();
			saveProfile();
		}

		@Override
		public boolean addAll(int index, Collection<? extends Macro> c) {
			for (Macro macro : c)
				putMacro(macro);
			boolean res = delegate.addAll(index, c);
			rebuild();
			return res;
		}

		@Override
		public void clear() {
			delegate.clear();
			keyStateMap.clear();
			rebuild();
		}

		@Override
		public Macro get(int index) {
			return delegate.get(index);
		}

		public Set<Macro> getMacros(KeyState keyState) {
			Set<Macro> macros = keyStateMap.get(keyState);
			if (macros == null)
				return Collections.emptySet();
			return macros;
		}

		@Override
		public Macro remove(int index) {
			Macro ret = delegate.remove(index);
			if (ret != null) {
				keyStateMap.get(ret.getState()).remove(ret);
			}
			rebuild();
			saveProfile();
			return ret;
		}

		@Override
		public Macro set(int index, Macro value) {
			Macro ret = delegate.set(index, value);
			if (ret != null) {
				keyStateMap.get(ret.getState()).remove(ret);
			}
			putMacro(value);
			rebuild();
			saveProfile();
			return ret;
		}

		@Override
		public int size() {
			return delegate.size();
		}

		protected void saveProfile() {
			if (profile != null) {
				try {
					profile.getSystem().getWriter().saveProfile(profile);
				} catch (IOException e) {
					throw new IllegalStateException("Failed to save activeProfiles.");
				}
			}
		}
	}

	private int bank;
	private transient List<Macro> delegate = new ArrayList<>();
	private transient Map<KeyState, Set<Macro>> keyStateMap = Collections.synchronizedMap(new HashMap<>());
	private transient Map<KeySequence, Macro> map = Collections.synchronizedMap(new HashMap<>());

	private MacroList macros = new MacroList();
	private String name;
	private transient List<Macro> normalHeldMacros = new ArrayList<>();
	private transient List<Macro> normalMacros = new ArrayList<>();
	private transient MacroProfile profile;
	private Map<String, Object> properties = new HashMap<>();
	private transient List<UInputMacro> uinputMacros = new ArrayList<>();

	MacroBank() {
	}

	MacroBank(MacroProfile profile, int bank) {
		this.bank = bank;
		this.profile = profile;
	}

	MacroBank(MacroProfile profile, MacroBank bank) {
		this.bank = bank.bank;
		this.properties.putAll(bank.properties);
		this.profile = profile;
		for (Macro macro : delegate) {
			Macro clone;
			try {
				clone = macro.clone();
			} catch (CloneNotSupportedException e) {
				throw new UnsupportedOperationException("Cannot clone macro.");
			}
			macros.add(clone);
		}
		rebuild();
	}

	/**
	 * Adds a macro to this bank.
	 *
	 * @param macro the macro
	 */
	public void add(Macro macro) {
		int idx = macros.indexOf(macro);
		if (idx == -1) {
			macros.add(macro);
		} else
			macros.set(idx, macro);
		rebuild();
	}

	/**
	 * Remove a macro from this bank.
	 * 
	 * @param macro macro to remove
	 */
	public void remove(Macro macro) {
		macros.remove(macro);
		rebuild();
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
		MacroBank other = (MacroBank) obj;
		if (bank != other.bank)
			return false;
		if (profile == null) {
			if (other.profile != null)
				return false;
		} else if (!profile.equals(other.profile))
			return false;
		return true;
	}

	/**
	 * Gets the bank.
	 *
	 * @return the bank
	 */
	public int getBank() {
		return bank;
	}

	/**
	 * Gets the macros.
	 *
	 * @return the macros
	 */
	public List<Macro> getMacros() {
		return macros;
	}

	/**
	 * Gets the macros.
	 *
	 * @param keyState the key state
	 * @return the macros
	 */
	public Set<Macro> getMacros(KeyState keyState) {
		Set<Macro> macros = keyStateMap.get(keyState);
		if (macros == null)
			return Collections.emptySet();
		return macros;
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
	 * Gets the normal held macros.
	 *
	 * @return the normal held macros
	 */
	public List<Macro> getNormalHeldMacros() {
		return normalHeldMacros;
	}

	/**
	 * Gets the normal macros.
	 *
	 * @return the normal macros
	 */
	public List<Macro> getNormalMacros() {
		return normalMacros;
	}

	/**
	 * Gets the profile.
	 *
	 * @return the profile
	 */
	public MacroProfile getProfile() {
		return profile;
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
	 * Gets the u input macros.
	 *
	 * @return the u input macros
	 */
	public List<UInputMacro> getUInputMacros() {
		return uinputMacros;
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
		result = prime * result + bank;
		result = prime * result + ((profile == null) ? 0 : profile.hashCode());
		return result;
	}

	/**
	 * Checks if is active.
	 *
	 * @return true, if is active
	 */
	public boolean isActive() {
		return getProfile().getSystem().getActiveBank(getProfile().getDevice()).equals(this);
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	public boolean isDefault() {
		return getProfile().getSystem().getDefaultBank(getProfile().getDevice()).equals(this);
	}

	/**
	 * Make this the currently active bank.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void makeActive() throws IOException {
		getProfile().getSystem().setActiveBank(this);
	}

	/**
	 * Removes the.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void remove() throws IOException {
		getProfile().remove(this);
	}

	/**
	 * Sets the bank.
	 *
	 * @param bank the new bank
	 */
	public void setBank(int bank) {
		this.bank = bank;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return name == null || name.length() == 0 ? "Bank " + ( bank + 1 ): name;
	}

	public void commit() {
		getProfile().commit();
	}

	protected void putMacro(Macro value) {
		synchronized (keyStateMap) {
			value.setBank(this);
			Set<Macro> l = keyStateMap.get(value.getActivatedBy().getState());
			if (l == null) {
				l = new LinkedHashSet<>();
				keyStateMap.put(value.getActivatedBy().getState(), l);
			}
			l.add(value);
		}
	}

	protected void rebuild() {

		synchronized (keyStateMap) {
			normalMacros.clear();
			normalHeldMacros.clear();
			uinputMacros.clear();
			map.clear();
			try {
				buildMacros(getProfile(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
			} catch (IOException e) {
				throw new IllegalStateException("Failed to rebuild optimised macro list.", e);
			}
			for (Macro m : macros) {
				map.put(m.getActivatedBy(), m);
			}
		}
	}

	void buildMacros(MacroProfile profile, List<KeySequence> macroKeys, List<KeySequence> heldMacroKeys,
			List<KeySequence> downMacroKeys) throws IOException {
		if (profile == null)
			profile = this.profile;
		if (profile == null)
			return;

		for (Macro m : getMacros(KeyState.UP)) {
			if (!macroKeys.contains(m.getActivatedBy())) {
				if (m.getType().isUInput())
					uinputMacros.add((UInputMacro) m);
				else
					normalMacros.add(m);
				macroKeys.add(m.getActivatedBy());
			}
			map.put(m.getActivatedBy(), m);
		}

		for (Macro m : profile.getMacros(KeyState.DOWN, bank)) {
			if (!downMacroKeys.contains(m.getActivatedBy())) {
				if (m.getType().isUInput())
					uinputMacros.add((UInputMacro) m);
				else
					normalMacros.add(m);
				downMacroKeys.add(m.getActivatedBy());
			}
			map.put(m.getActivatedBy(), m);
		}

		for (Macro m : profile.getMacros(KeyState.HELD, bank)) {
			if (!heldMacroKeys.contains(m.getActivatedBy())) {
				if (!m.getType().isUInput())
					normalHeldMacros.add(m);
				else
					uinputMacros.add((UInputMacro) m);
				heldMacroKeys.add(m.getActivatedBy());
			}
			map.put(m.getActivatedBy(), m);
		}

		if (profile.getBaseProfile() != null) {
			buildMacros(profile, macroKeys, heldMacroKeys, downMacroKeys);
		}
	}

	/**
	 * 
	 * Get all macros, including those in parent profiles. By default, the "root" is
	 * the active activeProfiles
	 * 
	 * @param activeProfiles root activeProfiles or None for active activeProfiles
	 * @param macros         list to append macros to.
	 * @param macroKeys      keys
	 * @param mappedToKey    boolean indicator whether to only find UINPUT type
	 *                       macros
	 * @param state          state
	 * @throws IOException on error
	 */
	List<Macro> getAllMacros(MacroProfile profile, List<Macro> macros, List<KeySequence> macroKeys, boolean mappedToKey,
			KeyState state) throws IOException {
		if (profile == null)
			profile = this.profile;

		if (state == null)
			state = KeyState.UP;

		for (Macro m : getMacros(state)) {
			if (!macroKeys.contains(m.getActivatedBy())) {
				if ((!mappedToKey && !m.getType().isUInput()) || (mappedToKey && m.getType().isUInput())) {
					macros.add(m);
					macroKeys.add(m.getActivatedBy());
				}
			}
		}
		if (profile.getBaseProfile() != null) {
			getAllMacros(profile.getBaseProfile(), macros, macroKeys, mappedToKey, state);
		}

		return macros;
	}

	void setProfile(MacroProfile profile) {
		this.profile = profile;
		rebuild();
	}

	public KeySequence getNextFreeActivationSequence() {
		for (KeyState state : KeyState.macroStates()) {
			for (EventCode code : EventCode.filteredForType(getProfile().getDevice().getSupportedInputEvents(), Type.EV_KEY)) {
				KeySequence keySequence = new KeySequence(state, code);
				if (!map.containsKey(keySequence))
					return keySequence;
			}
		}
		throw new IllegalStateException("No more activation sequences available.");
	}

	public Macro getMacro(KeySequence keySequence) {
		return map.get(keySequence);
	}

	public boolean contains(KeySequence keySequence) {
		return map.containsKey(keySequence);
	}

}
