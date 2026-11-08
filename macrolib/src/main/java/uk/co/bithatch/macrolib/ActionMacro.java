package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * The Class ActionMacro.
 */
public class ActionMacro extends Macro {
	final static Logger LOG = System.getLogger(ActionMacro.class.getName());

	private String action;

	{
		setType(TargetType.ACTION);
	}

	/**
	 * Instantiates a new action macro.
	 */
	public ActionMacro() {
	}

	/**
	 * Instantiates a new action macro.
	 *
	 * @param activatedBy the activated by
	 * @param action      the action
	 */
	public ActionMacro(KeySequence activatedBy, String action) {
		super(activatedBy);
		setAction(action);
	}

	public ActionMacro(Macro source) {
		super(source);
		if (source instanceof ActionMacro) {
			this.action = ((ActionMacro) source).action;
		}
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public Macro clone() throws CloneNotSupportedException {
		return new ActionMacro(this);
	}

	/**
	 * Do macro.
	 *
	 * @param execution the execution
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@Override
	public boolean doMacro(MacroExecution execution) throws Exception {
		LOG.log(Level.DEBUG, String.format("Action '%s'", action));

		ActionBinding binding = new ActionBinding(getProfile().getDevice(), action,
				execution.getMacro().getActivatedBy());
		if (!execution.getKeyboard().actionPerformed(binding)) {
			/*
			 * Not handled, let something else if there are any other handlers in the chain
			 */
			return false;
		} else {
			for (MacroKeyState k : execution.getKeyStates())
				k.setConsumeUntilRelease(true);
		}
		return true;
	}

	/**
	 * Gets the action.
	 *
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Sets the action.
	 *
	 * @param action the new action
	 */
	public void setAction(String action) {
		this.action = action;
	}

}
