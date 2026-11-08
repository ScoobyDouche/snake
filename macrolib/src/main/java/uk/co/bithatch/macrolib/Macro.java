package uk.co.bithatch.macrolib;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.co.bithatch.linuxio.EventCode;

/**
 * Represents a single macro in a activeProfiles.
 */
public abstract class Macro implements Comparable<Macro>, Cloneable {

	/**
	 * Parses the key list key.
	 *
	 * @param keyListKey the key list key
	 * @return the list
	 */
	public static List<EventCode> parseKeyListKey(String keyListKey) {
		return Arrays.asList(keyListKey.split("_")).stream().map((k) -> EventCode.valueOf(k))
				.collect(Collectors.toList());
	}

	private KeySequence activatedBy;
	private transient MacroBank bank;
	private String name;
	private double repeatDelay = MacroProfile.DEFAULT_REPEAT_DELAY;
	private RepeatMode repeatMode = RepeatMode.WHILE_HELD;
	private TargetType type = TargetType.NOTHING;

	/**
	 * Constructor.
	 */
	public Macro() {
	}

	/**
	 * Constructor.
	 *
	 * @param activatedBy the activated by
	 */
	public Macro(KeySequence activatedBy) {
		this.activatedBy = activatedBy;
	}

	/**
	 * Constructor
	 * 
	 * @param macro macro to base this one on
	 */
	Macro(Macro source) {
		bank = source.getBank();
		activatedBy = source.getActivatedBy();
		name = source.getName();
		repeatMode = source.getRepeatMode();
		type = source.getType();
		repeatDelay = source.getRepeatDelay();
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public abstract Macro clone() throws CloneNotSupportedException;

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(Macro o) {
		return activatedBy.compareTo(o.activatedBy);
	}

	/**
	 * Do macro.
	 *
	 * @param execution the execution
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public abstract boolean doMacro(MacroExecution execution) throws Exception;

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
		Macro other = (Macro) obj;
		if (activatedBy == null) {
			if (other.activatedBy != null)
				return false;
		} else if (!activatedBy.equals(other.activatedBy))
			return false;
		return true;
	}

	/**
	 * Gets the activated by.
	 *
	 * @return the activated by
	 */
	public KeySequence getActivatedBy() {
		return activatedBy;
	}

	/**
	 * Gets the bank.
	 *
	 * @return the bank
	 */
	public MacroBank getBank() {
		return bank;
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
	 * Gets the profile.
	 *
	 * @return the profile
	 */
	public MacroProfile getProfile() {
		return bank.getProfile();
	}

	/**
	 * Gets the repeat delay.
	 *
	 * @return the repeat delay
	 */
	public double getRepeatDelay() {
		return repeatDelay;
	}

	/**
	 * Gets the repeat mode.
	 *
	 * @return the repeat mode
	 */
	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public KeyState getState() {
		return activatedBy.getState();
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public TargetType getType() {
		return type;
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
		result = prime * result + ((activatedBy == null) ? 0 : activatedBy.hashCode());
		return result;
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
	 * Sets the repeat delay.
	 *
	 * @param repeatDelay the new repeat delay
	 */
	public void setRepeatDelay(double repeatDelay) {
		this.repeatDelay = repeatDelay;
	}

	/**
	 * Sets the repeat mode.
	 *
	 * @param repeatMode the new repeat mode
	 * @see #repeatMode(RepeatMode)
	 */
	public void setRepeatMode(RepeatMode repeatMode) {
		this.repeatMode = repeatMode;
	}
	
	/**
	 * Sets the repeat mode. Similar to {@link #setRepeatMode(RepeatMode)}, but
	 * returns an instance of this {@link Macro} for chaining.
	 *
	 * @param repeatMode the new repeat mode
	 * @return this
	 * @see #setRepeatMode(RepeatMode)
	 */
	public Macro repeatMode(RepeatMode repeatMode) {
		this.repeatMode = repeatMode;
		return this;
	}

	/**
	 * Sets the type.
	 *
	 * @param type the new type
	 */
	public void setType(TargetType type) {
		this.type = type;
	}

	void setBank(MacroBank bank) {
		this.bank = bank;
	}

	/**
	 * Remove this macro. The bank should be committed using
	 * {@link MacroBank#commit()} afterwards to save the change.
	 */
	public void remove() {
		getBank().remove(this);
	}

	/**
	 * Commit any unsaved changes.
	 */
	public void commit() {
		getBank().commit();
	}

	/**
	 * Get the best display name
	 * 
	 * @return display name
	 */
	public String getDisplayName() {
		return name == null ? getActivatedBy().toString() : name;
	}

}
