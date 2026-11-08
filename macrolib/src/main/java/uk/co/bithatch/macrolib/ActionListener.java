package uk.co.bithatch.macrolib;


/**
 * The listener interface for receiving action events.
 * The class that is interested in processing a action
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addActionListener</code> method. When
 * the action event occurs, that object's appropriate
 * method is invoked.
 */
public interface ActionListener {

	/**
	 * Action performed.
	 *
	 * @param binding the binding
	 * @return true, if successful
	 */
	boolean actionPerformed(ActionBinding binding);
}
