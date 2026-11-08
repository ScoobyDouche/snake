package uk.co.bithatch.macrolib;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice.Event;

/**
 * The Interface EventConsumer.
 */
public interface EventConsumer {

	/**
	 * Key received.
	 *
	 * @param key  the keys
	 * @param state the state
	 * @param event the event
	 */
	void keyReceived(EventCode key, KeyState state, Event event);
}
