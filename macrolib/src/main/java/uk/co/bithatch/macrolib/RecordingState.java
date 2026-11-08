package uk.co.bithatch.macrolib;

/**
 * The state of the recording system
 */
public enum RecordingState {
	/**
	 * No recording is occuring
	 */
	IDLE,
	/**
	 * Recording is active, but has been paused. Keys will pass through to their
	 * original target devices while paused.
	 */
	PAUSED,
	/**
	 * Macro recording has been started, and is awaiting the target key to map the
	 * macro to to be pressed.
	 */
	WAITING_FOR_TARGET_KEY,
	/**
	 * The target key is known, awaiting events to store in the macro.
	 */
	WAITING_FOR_EVENTS,
	/**
	 * An error occurred during recording.
	 */
	ERROR;

	/**
	 * Is recording active in this state.
	 * 
	 * @return recording active in this state
	 */
	public boolean isRecording() {
		switch(this) {
		case PAUSED:
		case WAITING_FOR_TARGET_KEY:
		case WAITING_FOR_EVENTS:
			return true;
		default:
			return false;
		}
	}
}