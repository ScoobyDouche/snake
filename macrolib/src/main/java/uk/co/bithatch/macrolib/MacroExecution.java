package uk.co.bithatch.macrolib;

import java.util.List;

import uk.co.bithatch.linuxio.InputDevice.Event;


/**
 * The Class MacroExecution.
 */
public class MacroExecution {

	private boolean cancelled;
	private Event event;
	private MacroKeyboard keyboard;
	private List<MacroKeyState> keyStates;
	private Macro macro;

	/**
	 * Instantiates a new macro execution.
	 *
	 * @param keyboard the keyboard
	 * @param keyStates the key states
	 * @param macro the macro
	 * @param event the event
	 */
	public MacroExecution(MacroKeyboard keyboard, List<MacroKeyState> keyStates, Macro macro, Event event) {
		this.keyboard = keyboard;
		this.keyStates = keyStates;
		this.macro = macro;
		this.event = event;
	}

	/**
	 * Cancel.
	 */
	public void cancel() {
		this.cancelled = true;
	}

	/**
	 * Gets the event.
	 *
	 * @return the event
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Gets the keyboard.
	 *
	 * @return the keyboard
	 */
	public MacroKeyboard getKeyboard() {
		return keyboard;
	}

	/**
	 * Gets the key states.
	 *
	 * @return the key states
	 */
	public List<MacroKeyState> getKeyStates() {
		return keyStates;
	}

	/**
	 * Gets the macro.
	 *
	 * @return the macro
	 */
	public Macro getMacro() {
		return macro;
	}

	/**
	 * Checks if is cancelled.
	 *
	 * @return true, if is cancelled
	 */
	public boolean isCancelled() {
		return cancelled;
	}
}
