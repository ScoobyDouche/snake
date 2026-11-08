package uk.co.bithatch.macrolib;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.bithatch.linuxio.EventCode;


/**
 * The Class MacroScriptExecution.
 */
public class MacroScriptExecution implements KeyListener {

	final static Logger LOG = System.getLogger(MacroScriptExecution.class.getName());

	private boolean allKeysUp;
	private boolean cancelled;
	private int down;
	private MacroExecution execution;
	private int l;
	private Map<String, Integer> labels;
	private ScriptMacro macro;
	private MacroSystem system;
	private List<EventCode> waitForKeys;
	private KeyState waitForState;

	/**
	 * Instantiates a new macro script execution.
	 *
	 * @param system the system
	 * @param macro the macro
	 * @param execution the execution
	 */
	public MacroScriptExecution(MacroSystem system, ScriptMacro macro, MacroExecution execution) {
		this.macro = macro;
		this.system = system;
		this.execution = execution;
		this.l = -1;
		this.waitForState = null;
		this.waitForKeys = new ArrayList<>();
		this.down = 0;
		this.allKeysUp = false;
		this.cancelled = false;

		/* First parse to get where the labels are */
		labels = new LinkedHashMap<>();
		for (int i = 0; i < macro.getScript().size(); i++) {
			String macroText = macro.getScript().get(i);
			String[] split = macroText.split(" ");
			String op = split[0].toLowerCase();
			if (op.equals("label") && split.length > 1)
				labels.put(split[1].toLowerCase(), i);
		}
	}

	/**
	 * Handle key.
	 *
	 * @param key the key
	 * @param state the state
	 * @param post the post
	 * @return true, if successful
	 */
	@Override
	public boolean handleKey(EventCode key, KeyState state, boolean post) {

		
		/*
		 * If we get the state we are waiting for, OR if we get an UP before getting a
		 * HELD, we remove this key from this key from the list we are waiting for
		 */
		if (state == waitForState || (KeyState.UP == state && KeyState.HELD == waitForState)) {
			waitForKeys.remove(key);
		}

		if (waitForKeys.isEmpty()) {
			/* Stop listening for further keys. We might re-add this after
			 * executing() again.
			 */
			execution.getKeyboard().removeListener(this);
			
			/* All keys are now in the required state */
			if (state == KeyState.UP && waitForState == KeyState.HELD)
				/* We should cancel execution now */
				cancelled = true;
			if (state == KeyState.UP)
				/* Make a note of the fact all triggering keys are now up */
				allKeysUp = true;
			
			try {
				execute();
				return true;
			}
			catch(IOException ioe) {
				LOG.log(Level.ERROR, "Failed to run paused script.", ioe);
			}
		}

		return false;
	}

	void execute() throws IOException {
		while (true) {
			if (down == 0 && (execution.isCancelled() || cancelled)) {
				LOG.log(Level.WARNING, String.format("Macro cancelled"));
				break;
			}
			l += 1;
			if (l == macro.getScript().size())
				break;
			String macroText = macro.getScript().get(l);
			String[] split = macroText.split(" ");
			String op = split[0].toLowerCase();
			if (split.length > 1) {
				String val = split[1];
				if (op.equals("goto")) {
					val = val.toLowerCase();
					if (labels.containsKey(val))
						l = labels.get(val);
					else
						LOG.log(Level.WARNING, String.format("Unknown goto label %s in macro script. Ignoring", val));
				} else if (op.equals("delay")) {
					if (!execution.isCancelled() && macro.getProfile().isSendDelays()
							&& !macro.getProfile().isFixedDelays()) {
						try {
							Thread.sleep(
									macro.getProfile().isFixedDelays()
											? (down > 0 ? macro.getProfile().getReleaseDelay()
													: macro.getProfile().getPressDelay())
											: Long.parseLong(val));
						} catch (InterruptedException e) {
						}
					}
				} else if (op.equals("press")) {
					if (down > 0)
						macro.releaseDelay();
					system.getDesktopIO().typeString(val, true);
					down += 1;
					macro.pressDelay();
				} else if (op.equals("release")) {
					system.getDesktopIO().typeString(val, false);
					down -= 1;
				} else if (op.equals("upress")) {
					if (split.length < 3) {
						LOG.log(Level.ERROR, String.format("Invalid operation in macro script. '%s'", macroText));
					} else {
						if (down > 0)
							macro.releaseDelay();
						down += 1;
						sendUinput(TargetType.valueOf(val.toUpperCase()), split[2], 1);
						macro.pressDelay();
					}
				} else if (op.equals("urelease")) {
					if (split.length < 3) {
						LOG.log(Level.ERROR, String.format("Invalid operation in macro script. '%s'", macroText));
					} else {
						down -= 1;
						sendUinput(TargetType.valueOf(val.toUpperCase()), split[2], 0);
					}
				} else if (op.equals("wait")) {
					if (allKeysUp) {
						LOG.log(Level.WARNING, String.format(
								"All keys for the macro %s are already up, the rest of the script will be ignored",
								macro.getName()));
						return;
					} else {
						val = val.toLowerCase();
						if (val.equals("release")) {
							if (macro.getActivatedBy().getState() == KeyState.UP) {
								LOG.log(Level.ERROR, String
										.format("WaitRelease cannot be used with macros that activate on release"));
							} else {
								waitForState = KeyState.UP;
								waitForKeys = new ArrayList<>(macro.getActivatedBy());
								execution.getKeyboard().addListener(this);
							}
						} else if (val.equals("hold")) {
							if (macro.getActivatedBy().getState() == KeyState.DOWN) {
								waitForState = KeyState.HELD;
								waitForKeys = new ArrayList<>(macro.getActivatedBy());
								execution.getKeyboard().addListener(this);
							} else {
								LOG.log(Level.ERROR, String.format(
										"WaitHold cannot be used with macros that activate on hold or release"));
							}
						} else {
							LOG.log(Level.ERROR, String.format("Wait may only have an argument of release or hold"));
						}
					}
				} else if (op.equals("label")) {
					/* Ignore label / comment */
					continue;
				} else {
					LOG.log(Level.ERROR, String.format("Invalid operation in macro script. '%s'", macroText));
				}
			}

			else {
				if (split.length > 0) {
					LOG.log(Level.ERROR, String.format("Insufficient arguments in macro script. '%s'", macroText));
				}
			}
		}
	}

	void sendUinput(TargetType target, String name, int value) throws IOException {
		// TODO check caps in UInput class?
//		if (system.getUInput().caps(code).contains(code))
		system.getUInput().emit(target, name, value);
//		else
//			LOG.log(Level.ERROR, String.format("Unknown uinput key %s.", code));
	}

}