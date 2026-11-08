package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * The Class SimpleMacro.
 */
public class SimpleMacro extends Macro {
	final static Logger LOG = System.getLogger(SimpleMacro.class.getName());

	private String macro;

	{
		setType(TargetType.SIMPLE);
	}

	/**
	 * Instantiates a new simple macro.
	 */
	public SimpleMacro() {
	}

	/**
	 * Instantiates a new simple macro.
	 *
	 * @param activatedBy the activated by
	 * @param macro       the macro
	 */
	public SimpleMacro(KeySequence activatedBy, String macro) {
		super(activatedBy);
		setMacro(macro);
	}

	SimpleMacro(SimpleMacro source) {
		super(source);
		this.macro = source.macro;
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public Macro clone() throws CloneNotSupportedException {
		return new SimpleMacro(this);
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
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, String.format("Simple macro '%s'", macro));
		boolean esc = false;
		int i = 0;

		long pressDelay = getProfile().isFixedDelays() ? getProfile().getPressDelay() : 0;
		long releaseDelay = getProfile().isFixedDelays() ? getProfile().getReleaseDelay() : 0;

		try {
			for (char c : macro.toCharArray()) {

				if (execution.isCancelled()) {
					LOG.log(Level.WARNING, "Macro cancelled.");
					break;
				}
				if (c == '\\' && !esc) {
					esc = true;
				} else {
					if (esc && c == 'p') {
						Thread.sleep(releaseDelay + pressDelay);
					} else {
						if (i > 0) {
							if (LOG.isLoggable(Level.DEBUG))
								LOG.log(Level.DEBUG, String.format("Release delay of %d", releaseDelay));
							Thread.sleep(releaseDelay);
						}

						if (esc && c == 't')
							c = '\t';
						else if (esc && c == 'r')
							c = '\r';
						else if (esc && c == 'n')
							c = '\r';
						else if (esc && c == 'b')
							c = '\b';
						else if (esc && c == 'e')
							c = 0x1b;
						else if (esc && c == '\\')
							c = '\\';

						String s = String.valueOf(c);
						if (LOG.isLoggable(Level.DEBUG))
							LOG.log(Level.DEBUG, String.format("Send '%s'", s));

						getProfile().getSystem().getDesktopIO().typeString(s, true);
						Thread.sleep(pressDelay);
						if (LOG.isLoggable(Level.DEBUG))
							LOG.log(Level.DEBUG, String.format("Press delay of %d", pressDelay));
						getProfile().getSystem().getDesktopIO().typeString(s, false);

						i += 1;
					}

					esc = false;
				}
			}
		} catch (Exception ie) {
			//
			LOG.log(Level.ERROR, "Failed to send X11 keypress.", ie);
		}
		return true;
	}

	/**
	 * Gets the macro.
	 *
	 * @return the macro
	 */
	public String getMacro() {
		return macro;
	}

	/**
	 * Sets the macro.
	 *
	 * @param macro the new macro
	 */
	public void setMacro(String macro) {
		this.macro = macro;
	}
}
