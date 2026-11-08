package uk.co.bithatch.macrolib;


/**
 * The Enum KeyState.
 */
public enum KeyState {
	DOWN, HELD, UP;
	
	/**
	 * Macro states.
	 *
	 * @return the key state[]
	 */
	public static KeyState[] macroStates() {
		return new KeyState[] { UP, HELD };
	}

	/**
	 * Checks if is macro state.
	 *
	 * @return true, if is macro state
	 */
	public boolean isMacroState() {
		switch(this) {
		case UP:
		case HELD:
			return true;
		default:
			return false;
		}
	}

}