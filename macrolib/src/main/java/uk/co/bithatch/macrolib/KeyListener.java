package uk.co.bithatch.macrolib;

import uk.co.bithatch.linuxio.EventCode;


/**
 * The listener interface for receiving key events.
 * The class that is interested in processing a key
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addKeyListener</code> method. When
 * the key event occurs, that object's appropriate
 * method is invoked.
 */
public interface KeyListener {
	
	/**
	 * Handle key.
	 *
	 * @param keys the keys
	 * @param state the state
	 * @param post the post
	 * @return true, if successful
	 */
	boolean handleKey(EventCode keys, KeyState state, boolean post);
}
