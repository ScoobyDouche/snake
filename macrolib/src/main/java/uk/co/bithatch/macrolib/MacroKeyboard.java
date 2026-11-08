package uk.co.bithatch.macrolib;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Type;
import uk.co.bithatch.linuxio.InputDevice.Event;

/**
 * This class deals with handling raw key presses from the user code, and
 * turning them into Macros or actions. The different types of macro are handled
 * accordingly, as well as the repetition functions.
 * 
 * All key events are handled on a queue (one per instance of a key handler).
 *
 */
public class MacroKeyboard implements /* ProfileListener, ActiveProfileListener, */ Closeable, EventConsumer {

	final static Logger LOG = System.getLogger(MacroKeyboard.class.getName());

	private static final double SIMULATED_INPUT_DELAY = 0.025;

	/**
	 * List of callbacks invoked when an action is activated by it's key combination
	 */
	private List<ActionListener> actionListeners = new ArrayList<>();

	private MacroDevice device;

	/**
	 * List of callbacks invoked for raw key handling. Normally plugins shouldn't
	 * use this, use actions instead
	 */
	private List<KeyListener> keyListeners = new ArrayList<>();

	private Map<EventCode, MacroKeyState> keyStates = new HashMap<>();
	private ScheduledFuture<?> macroRepeatTimer;
	private ScheduledExecutorService queue;
	private List<Macro> repeatMacros = new ArrayList<>();
	private MacroSystem system;

	/**
	 * Instantiates a new macro keyboard.
	 *
	 * @param system the system
	 * @param device the device
	 * @param queue  the queue
	 */
	public MacroKeyboard(MacroSystem system, MacroDevice device, ScheduledExecutorService queue) {
		this.system = system;
		this.device = device;
		this.queue = queue;
	}

	/**
	 * Adds the action listener.
	 *
	 * @param listener the listener
	 */
	public void addActionListener(ActionListener listener) {
		this.actionListeners.add(listener);
	}

	/**
	 * Stop handling keys.
	 */
	@Override
	public void close() {
	}

	/**
	 * Gets the key states.
	 *
	 * @return the key states
	 */
	public Map<EventCode, MacroKeyState> getKeyStates() {
		return keyStates;
	}

	/**
	 * This function starts processing of the provided keys, turning them into
	 * macros, actions and handling repetition. The key event will be placed on the
	 * queue, leaving this function to return immediately
	 *
	 * @param keys  list of keys to process
	 * @param state key state ID (g15driver.KEY_STATE_UP, _DOWN and _HELD)
	 * @param event the event
	 */
	@Override
	public void keyReceived(EventCode keys, KeyState state, Event event) {
		queue.execute(() -> {
			try {
				doKeyReceived(keys, state, event);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to handle received key.", e);
			}
		});
	}

	/**
	 * Removes the action listener.
	 *
	 * @param listener the listener
	 */
	public void removeActionListener(ActionListener listener) {
		this.actionListeners.remove(listener);
	}

	boolean actionPerformed(ActionBinding binding) {
		for (int i = actionListeners.size() - 1; i >= 0; i--) {
			if (actionListeners.get(i).actionPerformed(binding))
				return true;
		}
		return false;
	}

	void addListener(KeyListener listener) {
		keyListeners.add(listener);
	}

	/**
	 * Cancel the currently pending macro repeat
	 */
	void cancelMacroRepeatTimer() {
		if (macroRepeatTimer != null) {
			macroRepeatTimer.cancel(false);
			macroRepeatTimer = null;
		}
	}

	/**
	 * 
	 * Sanity check
	 * 
	 * Keyword arguments:
	 * 
	 * @param newState new state ID
	 * @param keyState key state object
	 * @return
	 */
	boolean checkKeyState(KeyState newState, MacroKeyState keyState) {
		if (newState == KeyState.UP && keyState.getState() != KeyState.DOWN && keyState.getState() != KeyState.HELD)
			return false;
		if (newState == KeyState.HELD && (keyState.getState() == null || keyState.getState() == KeyState.UP))
			/* Received key held state before receiving key down. */
			return false;

		return true;
	}

	/**
	 * 
	 * Maintains the "key state" table, which holds what state each key is currently
	 * in.
	 * 
	 * This function will return the number of state changes, so this key event may
	 * be ignored if it is no longer appropriate (i.e. a hold timer event for keys
	 * that are now released)
	 * 
	 * @param key   single key
	 * @param state state
	 * @param event event
	 * @return handled
	 */
	boolean configureKeyState(EventCode key, KeyState state, Event event) {
		/* For now, just ignore everything that is not a key or a button */
		if(key.type() != Type.EV_KEY) {
			return false;
		}

		if (state == KeyState.HELD && !keyStates.containsKey(key)) {
			/*
			 * All keys were released before the HOLD timer kicked in, so we totally ignore
			 * this key
			 */
			return false;
		} else {
			MacroKeyState keyState = keyStates.get(key);
			if (keyState == null)
				keyStates.put(key, keyState = new MacroKeyState(key));

			/* This is a new key press, so reset this key's consumed state */
			keyState.setConsumed(false);

			/* Check the sanity of the key press */
			checkKeyState(state, keyState);
			keyState.setState(state);

			if (state == KeyState.DOWN)
				/*
				 * Key is now down, let's set up a timer to produce a held event
				 */
				keyState.setTimer(queue.schedule(() -> {
					try {
						doKeyReceived(key, KeyState.HELD, event);
					} catch (Exception e) {
						throw new IllegalStateException("Failed to handle received key.", e);
					}
				}, (long) (system.getKeyHoldDelay() * 1000.0), TimeUnit.MILLISECONDS));
			else if (state == KeyState.UP)
				/*
				 * Now the key is up, cancel the HELD timer if one exists.
				 */
				keyState.cancelTimer();

			return true;
		}
	}

	/**
	 * 
	 * Mark as consumed so they don't get activated again if other key's are pressed
	 * or released while this macro is active
	 * 
	 * @param key_states list of KeyState objects to mark as consumed
	 */
	void consumeKeys(List<MacroKeyState> key_states) {
		for (MacroKeyState k : key_states)
			k.setConsumed(true);
	}

	void defeatRelease(List<MacroKeyState> key_states) {
		for (MacroKeyState k : key_states) {
			k.setDefeatRelease(true);
			k.cancelTimer();
		}
	}

	/**
	 * Actual handling of key events.
	 * 
	 * @param key      key
	 * @param state_id key state (g15driver.KEY_STATE_UP, _DOWN and _HELD)
	 * @param event    event
	 * @throws IOException on error
	 */
	void doKeyReceived(EventCode key, KeyState state, Event event) throws Exception {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, String.format("Received key %s sequence", new KeySequence(state, key)));

		MacroBank bank = system.getActiveBank(device);

		/*
		 * See if the screen itself, or the plugins, want to handle the key. This is the
		 * legacy method of key handling, the preferred method now is actions which is
		 * handled below. However, this is still useful for plugins that want to take
		 * over key handling, such as screensaver which disables all keys while it is
		 * active.
		 */
		if (handleKey(key, state, false)) {
			return;
		}

		/*
		 * Deal with each key separately, this keeps it simpler
		 */
		boolean handled = false;
		/*
		 * Now set up the macro key state. This is where we decide what macros and
		 * actions to activate.
		 */
		if (configureKeyState(key, state, event)) {

			/*
			 * Do uinput macros first. These are treated slightly differently, because a
			 * press of the Macro key equals a "press" of the virtual key, a release of the
			 * Macro key equals a "release" of the virtual key etc.
			 */
			handled = handled || handleUinputMacros(bank);

			/*
			 * Now the ordinary macros, processed on key_up
			 */
			handled = handled || handleNormalMacros(event, bank);

			/*
			 * Now the actions
			 */
			handled = handled || handleActions();
		} else {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("No macros mapped for '%s' in state %s", key, state));
		}

		/*
		 * Now do the legacy 'post' handling.
		 */
		if (handleKey(key, state, true)) {
			return;
		}

		/*
		 * When ALL keys are UP, clear out the state
		 */
		int up = 0;
		for (Map.Entry<EventCode, MacroKeyState> en : keyStates.entrySet()) {
			if (en.getValue().getState() == KeyState.UP)
				up++;
		}
		if (up > 0 && up == keyStates.size())
			keyStates.clear();

		/*
		 * If nothing handled the key, the pass it on to an appropriate virtual device
		 */
		if (state != KeyState.HELD && !handled) {
			TargetType targetType = TargetType.forEvent(event.getCode());
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Not handled, passing on %s %s, value %d to %s", key, state,
						event.getValue(), targetType));
			system.getUInput().emit(targetType, key, event.getValue());
		}
	}

	/*
	 * 
	 * This handles the default action bindings. The actions may have already
	 * re-mapped as a macro, in which case they will be ignored here.
	 */
	boolean handleActions() {
		Map<String, ActionBinding> actionKeys = device.getActionKeys();
		boolean handled = false;
		if (actionKeys != null) {
			for (String action : actionKeys.keySet()) {
				ActionBinding binding = actionKeys.get(action);
				int f = 0;
				for (EventCode k : binding.getKeys()) {
					if (keyStates.containsKey(k) && binding.getState() == keyStates.get(k).getState()
							&& !keyStates.get(k).isConsumedState())
						f += 1;
				}
				if (f == binding.getKeys().size()) {
					actionPerformed(binding);
					handled = true;
					for (EventCode k : binding.getKeys())
						keyStates.get(k).setConsumeUntilRelease(true);
				}
			}
		}
		return handled;
	}

	/**
	 * 
	 * Send the key press to various handlers. This is for plugins and other code
	 * that needs to completely take over the macro keys, for general key handling
	 * "Actions" should be used instead.
	 * 
	 * @param keys  keys
	 * @param state state
	 * @param post  post handling
	 * @return handled
	 */
	boolean handleKey(EventCode keys, KeyState state, boolean post) {
		for (int i = keyListeners.size() - 1; i >= 0; i--) {
			if (keyListeners.get(i).handleKey(keys, state, post))
				return true;
		}

		return false;
	}

	void handleMacro(Macro macro, KeyState state, List<MacroKeyState> keyStates, boolean repetition, Event event)
			throws Exception {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, String.format("Executing macro '%s' in state %s", macro.getActivatedBy(), state));

		consumeKeys(keyStates);
		double delay = macro.getRepeatDelay() != -1 ? macro.getRepeatDelay() : 0.1d;
		if (macro.getRepeatMode() == RepeatMode.TOGGLE && state == KeyState.UP) {
			if (repeatMacros.contains(macro) && !repetition) {
				/* Key pressed again, so stop repeating */
				cancelMacroRepeatTimer();
				repeatMacros.remove(macro);
			} else {
				if (!repeatMacros.contains(macro) && !repetition)
					repeatMacros.add(macro);
				else
					processMacro(macro, state, keyStates, event);

				/* We test again because a toggle might have stopped the repeat */
				if (repeatMacros.contains(macro)) {
					macroRepeatTimer = queue.schedule(() -> {
						try {
							handleMacro(macro, state, keyStates, true, event);
						} catch (Exception e) {
							LOG.log(Level.ERROR, "Failed to handle macro.", e);
						}
					}, (long) (delay * 1000.0), TimeUnit.MILLISECONDS);
				}
			}
		} else if (macro.getRepeatMode() == RepeatMode.WHILE_HELD && state != KeyState.DOWN) {
			if (state == KeyState.UP && repeatMacros.contains(macro) && !repetition) {
				/* Key released again, so stop repeating */
				cancelMacroRepeatTimer();
				repeatMacros.remove(macro);
			} else {

				if (state == KeyState.HELD && !repeatMacros.contains(macro) && !repetition)
					repeatMacros.add(macro);

				if (!macro.getActivatedBy().getState().equals(KeyState.DOWN)
						|| (macro.getActivatedBy().getState().equals(KeyState.DOWN) && state != KeyState.UP))
					processMacro(macro, state, keyStates, event);

				/* We test again because a toggle might have stopped the repeat */
				if (repeatMacros.contains(macro)) {
					macroRepeatTimer = queue.schedule(() -> {
						try {
							handleMacro(macro, KeyState.HELD, keyStates, true, event);
						} catch (Exception e) {
							LOG.log(Level.ERROR, "Failed to handle macro.", e);
						}
					}, (long) (delay * 1000.0), TimeUnit.MILLISECONDS);
				}
			}
		} else if (state == KeyState.DOWN && macro.getActivatedBy().getState() == KeyState.DOWN) {
			processMacro(macro, state, keyStates, event);
		} else if (state == KeyState.UP && macro.getActivatedBy().getState() == KeyState.UP) {
			processMacro(macro, state, keyStates, event);
		} else if (state == KeyState.HELD && macro.getActivatedBy().getState() == KeyState.HELD) {
			processMacro(macro, state, keyStates, event);

			/*
			 * Also defeat the key release so any normal KeyState.`UP macros don't get
			 * activated as well
			 */
			defeatRelease(keyStates);
		}
	}

	/*
	 * 
	 * First check for any KEY_STATE_HELD macros. We do these first so KEY_STATE_UP
	 * macros don't consume the key states
	 * 
	 * @param event event
	 * 
	 * @param bank active bank
	 * 
	 * @return handled
	 */
	boolean handleNormalMacros(Event event, MacroBank bank) throws Exception {
		boolean handled = false;
		for (Macro m : bank.getNormalHeldMacros()) {
			List<MacroKeyState> held = new ArrayList<>();
			for (EventCode k : m.getActivatedBy()) {
				if (keyStates.containsKey(k)) {
					MacroKeyState keyState = keyStates.get(k);
					if (!keyState.isConsumedState() && keyState.getState() == KeyState.HELD)
						held.add(keyState);
				}
			}

			if (held.size() == m.getActivatedBy().size()) {
				handleMacro(m, KeyState.HELD, held, false, event);
				handled = true;
			}
		}

		/*
		 * Search for all the non-uinput macros that would be activated by the current
		 * key state. In this case, KEY_STATE_UP macros are looked for
		 */
		for (Macro m : bank.getNormalMacros()) {
			List<MacroKeyState> up = new ArrayList<>();
			List<MacroKeyState> held = new ArrayList<>();
			List<MacroKeyState> down = new ArrayList<>();
			for (EventCode k : m.getActivatedBy()) {
				if (keyStates.containsKey(k)) {
					MacroKeyState keyState = keyStates.get(k);
					if (!keyState.isConsumedState() && keyState.getState() == KeyState.DOWN)
						down.add(keyState);
					if (!keyState.isConsumedState() && keyState.getState() == KeyState.UP
							&& !keyState.isDefeatRelease())
						up.add(keyState);
					if (!keyState.isConsumedState() && keyState.getState() == KeyState.HELD)
						held.add(keyState);
				}
			}

			if (up.size() == m.getActivatedBy().size()) {
				handleMacro(m, KeyState.UP, up, false, event);
				handled = true;
			}
			if (down.size() == m.getActivatedBy().size()) {
				handleMacro(m, KeyState.DOWN, down, false, event);
				handled = true;
			}
			if (held.size() == m.getActivatedBy().size()) {
				handleMacro(m, KeyState.HELD, held, false, event);
				handled = true;
			}
		}

		return handled;
	}

	void handleUinputMacro(UInputMacro macro, KeyState state, List<MacroKeyState> key_states) throws IOException {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG,
					String.format("Executing UInput macro '%s' in state %s", macro.getActivatedBy(), state));

		EventCode uc = ((UInputMacro) macro).getCode();

		consumeKeys(key_states);
		if (state == KeyState.UP) {
			if (repeatMacros.contains(macro) && macro.getRepeatMode() == RepeatMode.WHILE_HELD) {
				repeatMacros.remove(macro);
				system.getUInput().emit(macro.getType(), uc, 0);
			} else if (macro.getRepeatMode() == RepeatMode.WHILE_HELD && macro.getRepeatDelay() == -1) {
				if (!macro.getActivatedBy().getState().equals(KeyState.HELD)
						&& (macro.getBank().contains(new KeySequence(KeyState.UP, macro.getActivatedBy()))
								|| macro.getBank().contains(new KeySequence(KeyState.HELD, macro.getActivatedBy())))) {
					/*
					 * If there are were no macros that activate when the key was pressed, then do a
					 * press now of that key
					 */
					system.getUInput().type(macro.getType(), uc);
				} else
					system.getUInput().emit(macro.getType(), uc, 0);
			} else if (macro.getRepeatMode() == RepeatMode.NONE
					&& macro.getActivatedBy().getState().equals(KeyState.UP)) {
				/*
				 * If the repeat mode was REPEAT_NONE, and this is activated by a DOWN state,
				 * then we won't have done the key press yet, so do it now.
				 */
				system.getUInput().type(macro.getType(), uc);
			} else if (macro.getRepeatMode() == RepeatMode.TOGGLE
					&& macro.getActivatedBy().getState().equals(KeyState.UP)) {

				if (repeatMacros.contains(macro)) {
					/* Stop repeating */
					repeatMacros.remove(macro);
				} else {
					/* Start repeating */
					repeatMacros.add(macro);
					repeatUinput(macro, uc, false,
							macro.getRepeatDelay() == -1 ? SIMULATED_INPUT_DELAY : macro.getRepeatDelay());
				}
			} else
				system.getUInput().emit(macro.getType(), uc, 0);
		} else if (state == KeyState.DOWN) {
			if (repeatMacros.contains(macro)) {
				if (macro.getRepeatMode() == RepeatMode.TOGGLE && macro.getRepeatDelay() != -1)
					/*
					 * For REPEAT_TOGGLE mode with custom repeat rate, we now cancel the repeat
					 * timer and defeat the key release.
					 */
					repeatMacros.remove(macro);
				else if(macro.getActivatedBy().getState().equals(KeyState.DOWN)) {
					/*
					 * For all modes with default repeat rate, we will send a release if this is the
					 * second press. We also defeat the 2nd release.
					 */
					system.getUInput().emit(macro.getType(), uc, 0);
					repeatMacros.remove(macro);
					defeatRelease(key_states);
				}
			} else {
				if (macro.getRepeatMode() == RepeatMode.TOGGLE) {
					/*
					 * Start repeating
					 */
					if (!repeatMacros.contains(macro)) {
						if (macro.getRepeatDelay() != -1) {
							/*
							 * For the default delay, simulate a repeat
							 */
							defeatRelease(key_states);
							repeatMacros.add(macro);
							repeatUinput(macro, uc, false, macro.getRepeatDelay());
						} else if (macro.getActivatedBy().getState().equals(KeyState.DOWN)) {
							/*
							 * For the custom delay, send the key press now. We send the first when it is
							 * actually released, then start sending further repeats on a timer
							 */
							defeatRelease(key_states);
							repeatMacros.add(macro);
							system.getUInput().emit(macro.getType(), uc, 1);
//							defeatRelease(key_states);
						}
					}
				} else if (macro.getRepeatMode() == RepeatMode.NONE) {
					/*
					 * For NO_REPEAT macros we type the key now (press then release), and defeat the
					 * actual key release that will come later.
					 */
					if (macro.getActivatedBy().getState().equals(KeyState.DOWN))
						sendUinputKeypress(macro, uc, false);
				} else if (macro.getRepeatMode() == RepeatMode.WHILE_HELD && macro.getRepeatDelay() != -1) {
					sendUinputKeypress(macro, uc, false);
				} else {
					/*
					 * Only press now if there are no macros that activate when the key is released
					 * (or held)
					 */
					if (!macro.getBank().contains(new KeySequence(KeyState.UP, macro.getActivatedBy()))
							&& !macro.getBank().contains(new KeySequence(KeyState.HELD, macro.getActivatedBy())))
						system.getUInput().emit(macro.getType(), uc, 1);
				}
			}
		} else if (state == KeyState.HELD) {
			if (macro.getRepeatMode() == RepeatMode.WHILE_HELD) {
				if (macro.getRepeatDelay() != -1) {
					repeatMacros.add(macro);
					repeatUinput(macro, uc, false, macro.getRepeatDelay());
				} else if (macro.getActivatedBy().getState() == KeyState.HELD) {
					repeatMacros.add(macro);
					repeatUinput(macro, uc, false, SIMULATED_INPUT_DELAY);
				}
			} else if (macro.getRepeatMode() == RepeatMode.NONE && macro.getActivatedBy().getState() == KeyState.HELD) {
				system.getUInput().type(macro.getType(), uc);
				defeatRelease(key_states);
			}
		}
	}

	/**
	 * 
	 * Search for all the uinput macros that would be activated by the current key
	 * state, and emit events of the same type.
	 * 
	 * @throws IOException on error
	 */
	boolean handleUinputMacros(MacroBank bank) throws IOException {
		boolean uinputRepeat = false;
		boolean handled = false;
		for (UInputMacro m : bank.getUInputMacros()) {
			List<MacroKeyState> down = new ArrayList<>();
			List<MacroKeyState> up = new ArrayList<>();
			List<MacroKeyState> held = new ArrayList<>();
			for (EventCode k : m.getActivatedBy()) {
				if (keyStates.containsKey(k)) {
					MacroKeyState keyState = keyStates.get(k);
					if (!keyState.isConsumed()) {
						if (keyState.getState() == KeyState.UP && !keyState.isDefeatRelease())
							up.add(keyState);
						if (keyState.getState() == KeyState.DOWN)
							down.add(keyState);
						if (keyState.getState() == KeyState.HELD)
							held.add(keyState);
					}
				}
			}

			if (down.size() == m.getActivatedBy().size()) {
				handleUinputMacro(m, KeyState.DOWN, down);
				handled = true;
			}
			if (up.size() == m.getActivatedBy().size()) {
				handleUinputMacro(m, KeyState.UP, up);
				handled = true;
			}
			if (held.size() == m.getActivatedBy().size()) {
				handleUinputMacro(m, KeyState.HELD, held);
				uinputRepeat = true;
				handled = true;
			}
		}

		/*
		 * Simulate a uinput repeat by just handling an empty key list. No keys have
		 * changed state, so we should just keep hitting this reschedule until they do
		 */
		if (uinputRepeat)
			queue.schedule(() -> {
				try {
					handleUinputMacros(bank);
				} catch (IOException e) {
					throw new IllegalStateException("Failed to handle UInput macro.", e);
				}
			}, (long) (SIMULATED_INPUT_DELAY * 1000.0), TimeUnit.MILLISECONDS);

		return handled;
	}

	void processMacro(Macro macro, KeyState state, List<MacroKeyState> keyStates, Event event) throws Exception {
		macro.doMacro(new MacroExecution(this, keyStates, macro, event));
	}

	void removeListener(KeyListener listener) {
		keyListeners.remove(listener);
	}

	void repeatUinput(Macro macro, EventCode uc, boolean uinput_repeat, double delay) throws IOException {
		if (repeatMacros.contains(macro)) {
			sendUinputKeypress(macro, uc, uinput_repeat);
			queue.schedule(() -> {
				try {
					repeatUinput(macro, uc, uinput_repeat, delay);
				} catch (IOException e) {
					throw new IllegalStateException("Failed to repeat.", e);
				}
			}, (long) (delay * 1000.0), TimeUnit.MILLISECONDS);
		}
	}

	void sendUinputKeypress(Macro macro, EventCode uc, boolean uinput_repeat) throws IOException {
		if (uinput_repeat)
			system.getUInput().emit(macro.getType(), uc, 2);
		else {
			system.getUInput().type(macro.getType(), uc);
		}
	}
}
