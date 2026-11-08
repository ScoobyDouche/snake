package uk.co.bithatch.macrolib;

import java.lang.System.Logger;


/**
 * The Class NoopMacro.
 */
public class NoopMacro extends Macro {
	final static Logger LOG = System.getLogger(NoopMacro.class.getName());

	{
		setType(TargetType.NOTHING);
	}

	/**
	 * Instantiates a new noop macro.
	 */
	public NoopMacro() {
	}

	/**
	 * Instantiates a new noop macro.
	 *
	 * @param activatedBy the activated by
	 */
	public NoopMacro(KeySequence activatedBy) {
		super(activatedBy);
	}

	public NoopMacro(Macro source) {
		super(source);
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public Macro clone() throws CloneNotSupportedException {
		return new NoopMacro(this);
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
		return true;
	}

}
