package uk.co.bithatch.macrolib;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice.Event;

public class RecordedEvent {
	private EventCode key;
	private KeyState state;
	private Event event;
	private MacroDevice device;

	RecordedEvent(EventCode key, KeyState state, Event event, MacroDevice device) {
		super();
		this.key = key;
		this.state = state;
		this.event = event;
		this.device = device;
	}

	public EventCode getKey() {
		return key;
	}

	public void setKey(EventCode key) {
		this.key = key;
	}

	public KeyState getState() {
		return state;
	}

	public void setState(KeyState state) {
		this.state = state;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public MacroDevice getDevice() {
		return device;
	}

	public void setDevice(MacroDevice device) {
		this.device = device;
	}

	@Override
	public String toString() {
		return "RecordedEvent [key=" + key + ", state=" + state + ", event=" + event + ", device=" + device + "]";
	}

}
