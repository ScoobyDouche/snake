package uk.co.bithatch.macrolib;

import java.util.Iterator;

import uk.co.bithatch.linuxio.EventCode;

public class RecordingSession {

	private RecordingState recordingState = RecordingState.IDLE;
	private Exception recordingError = null;
	private int count;
	private EventCode target;
	private MacroDevice targetDevice;
	private RecordingState stateBeforePaused;

	public RecordingState getRecordingState() {
		return recordingState;
	}

	void setRecordingState(RecordingState recordingState) {
		this.recordingState = recordingState;
	}

	public Exception getRecordingError() {
		return recordingError;
	}

	void setRecordingError(Exception recordingError) {
		this.recordingError = recordingError;
	}

	public MacroDevice getTargetDevice() {
		return targetDevice;
	}

	void setTargetDevice(MacroDevice targetDevice) {
		this.targetDevice = targetDevice;
	}

	void setCount(int count) {
		this.count = count;
	}

	void setTarget(EventCode target) {
		this.target = target;
	}

	public int getCount() {
		return count;
	}

	public Iterator<RecordedEvent> getEvents() {
		throw new UnsupportedOperationException("TODO");
	}

	public EventCode getTarget() {
		return target;
	}

	void start() {
		recordingState = RecordingState.WAITING_FOR_TARGET_KEY;
	}

	void stop() {
		recordingState = RecordingState.IDLE;
	}

	void pause() {
		if (stateBeforePaused == null) {
			stateBeforePaused = recordingState;
			recordingState = RecordingState.PAUSED;
		} else
			throw new IllegalStateException();
	}

	void unpause() {
		try {
			recordingState = stateBeforePaused;
		}
		finally {
			stateBeforePaused = null;
		}
	}

}
