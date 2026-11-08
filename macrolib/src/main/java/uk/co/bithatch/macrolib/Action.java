package uk.co.bithatch.macrolib;


/**
 * The Class Action.
 */
public class Action implements ActionListener {

	private final ActionListener delegate;
	private final String id;

	/**
	 * Instantiates a new action.
	 *
	 * @param id the id
	 */
	public Action(String id) {
		this(id, null);
	}

	/**
	 * Instantiates a new action.
	 *
	 * @param id the id
	 * @param delegate the delegate
	 */
	public Action(String id, ActionListener delegate) {
		this.delegate = delegate;
		this.id = id;
	}

	/**
	 * Action performed.
	 *
	 * @param binding the binding
	 * @return true, if successful
	 */
	@Override
	public boolean actionPerformed(ActionBinding binding) {
		if (delegate == null)
			return false;
		else
			return delegate.actionPerformed(binding);
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getId();
	}

}
