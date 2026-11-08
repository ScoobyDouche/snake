package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The Class ScriptMacro.
 */
public class ScriptMacro extends Macro {

	final static Logger LOG = System.getLogger(ScriptMacro.class.getName());

	private List<String> script;

	{
		setType(TargetType.SCRIPT);
	}

	/**
	 * Instantiates a new script macro.
	 */
	public ScriptMacro() {
	}
	
	/**
	 * Instantiates a new script macro.
	 *
	 * @param activatedBy the activated by
	 * @param script the script
	 */
	public ScriptMacro(KeySequence activatedBy, String... script) {
		super(activatedBy);
		setScript(Arrays.asList(script));
	}

	public ScriptMacro(Macro source) {
		super(source);
		if(source instanceof ScriptMacro && ((ScriptMacro)source).script != null)
			this.script = new ArrayList<>(((ScriptMacro)source).script);
		else
			this.script = new ArrayList<>();
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public Macro clone() throws CloneNotSupportedException {
		return new ScriptMacro(this);
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
		new MacroScriptExecution(getProfile().getSystem(), this, execution).execute();
		return true;
	}

	/**
	 * Gets the script.
	 *
	 * @return the script
	 */
	public List<String> getScript() {
		return script;
	}

	/**
	 * Sets the script.
	 *
	 * @param script the new script
	 */
	public void setScript(List<String> script) {
		this.script = script;
	}

	/**
	 * Sets the script.
	 *
	 * @param script the new script
	 */
	public void setScript(String script) {
		setScript(Arrays.asList(script.split("\n")));
	}

	void pressDelay() {
		long delay = getProfile().isFixedDelays() ? getProfile().getPressDelay() : 0;
		LOG.log(Level.DEBUG, String.format("Press delay of %d", delay));
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}

	void releaseDelay() {
		long delay = getProfile().isFixedDelays() ? getProfile().getReleaseDelay() : 0;
		LOG.log(Level.DEBUG, String.format("Release delay of %d", delay));
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}
}