package uk.co.bithatch.macrolib;

import java.util.concurrent.ScheduledFuture;

import uk.co.bithatch.linuxio.EventCode;


/**
 * Holds the current state of a single macro key.
 */
public class MacroKeyState {
	private boolean consumed;
	private boolean consumeUntilRelease;
	private boolean defeatRelease;
	private EventCode key;
	private KeyState state;
	private ScheduledFuture<?> timer;

	/**
	 * Instantiates a new macro key state.
	 *
	 * @param key the key
	 */
	public MacroKeyState(EventCode key) {
		this.key = key;
	}

	/**
	 * Cancel timer.
	 */
	public void cancelTimer() {
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public EventCode getKey() {
		return key;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public KeyState getState() {
		return state;
	}

	/**
	 * Gets the timer.
	 *
	 * @return the timer
	 */
	public ScheduledFuture<?> getTimer() {
		return timer;
	}

	/**
	 * Checks if is consumed.
	 *
	 * @return true, if is consumed
	 */
	public boolean isConsumed() {
		return consumed;
	}

	/**
	 * Checks if is consumed state.
	 *
	 * @return true, if is consumed state
	 */
	public boolean isConsumedState() {
		return consumed || consumeUntilRelease;
	}

	/**
	 * Checks if is consume until release.
	 *
	 * @return true, if is consume until release
	 */
	public boolean isConsumeUntilRelease() {
		return consumeUntilRelease;
	}

	/**
	 * Checks if is defeat release.
	 *
	 * @return true, if is defeat release
	 */
	public boolean isDefeatRelease() {
		return defeatRelease;
	}

	/**
	 * Sets the consumed.
	 *
	 * @param consumed the new consumed
	 */
	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}

	/**
	 * Sets the consume until release.
	 *
	 * @param consumeUntilRelease the new consume until release
	 */
	public void setConsumeUntilRelease(boolean consumeUntilRelease) {
		this.consumeUntilRelease = consumeUntilRelease;
	}

	/**
	 * Sets the defeat release.
	 *
	 * @param defeatRelease the new defeat release
	 */
	public void setDefeatRelease(boolean defeatRelease) {
		this.defeatRelease = defeatRelease;
	}

	/**
	 * Sets the key.
	 *
	 * @param key the new key
	 */
	public void setKey(EventCode key) {
		this.key = key;
	}

	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	public void setState(KeyState state) {
		this.state = state;
	}

	/**
	 * Sets the timer.
	 *
	 * @param timer the new timer
	 */
	public void setTimer(ScheduledFuture<?> timer) {
		this.timer = timer;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "MacroKeyState [key=" + key + ", state=" + state + ", timer=" + timer + ", consumed=" + consumed
				+ ", defeatRelease=" + defeatRelease + ", consumeUntilRelease=" + consumeUntilRelease + "]";
	}

}