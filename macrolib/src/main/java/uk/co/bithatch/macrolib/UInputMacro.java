package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import uk.co.bithatch.linuxio.EventCode;

/**
 * The Class UInputMacro.
 */
public class UInputMacro extends Macro {
	final static Logger LOG = System.getLogger(UInputMacro.class.getName());

	private EventCode code;
	private boolean passthrough = true;
	private int value = 0;

	/**
	 * Instantiates a new u input macro.
	 */
	public UInputMacro() {
		super();
	}

	/**
	 * Instantiates a new uinput macro.
	 *
	 * @param activatedBy the activated by
	 * @param code        the code
	 * @param target      the target
	 */
	public UInputMacro(KeySequence activatedBy, EventCode code, TargetType target) {
		super(activatedBy);
		setCode(code);
		setType(target == null ? TargetType.KEYBOARD : target);
	}

	/**
	 * Instantiates a new uinput macro.
	 *
	 * @param activatedBy the activated by
	 * @param code        the code
	 */
	public UInputMacro(KeySequence activatedBy, EventCode code) {
		this(activatedBy, code, null);
	}


	/**
	 * Instantiates a new uinput macro.
	 *
	 * @param source        the macro to base this one on
	 */
	public UInputMacro(Macro source) {
		super(source);
		if (source instanceof UInputMacro) {
			this.code = ((UInputMacro) source).code;
			this.value = ((UInputMacro) source).value;
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
		return new UInputMacro(this);
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
		LOG.log(Level.WARNING, String.format("UInput '%s'", getType(), code));
		getProfile().getSystem().getUInput().emit(getType(), code,
				value == Integer.MIN_VALUE ? execution.getEvent().getValue() : value);
		return true;
	}

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public EventCode getCode() {
		return code;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Checks if is passthrough.
	 *
	 * @return true, if is passthrough
	 */
	public boolean isPassthrough() {
		return passthrough;
	}

	/**
	 * Sets the code.
	 *
	 * @param code the new code
	 */
	public void setCode(EventCode code) {
		this.code = code;
	}

	/**
	 * Sets the passthrough.
	 *
	 * @param passthrough the new passthrough
	 */
	public void setPassthrough(boolean passthrough) {
		this.passthrough = passthrough;
	}

	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public void setValue(int value) {
		this.value = value;
	}

}
