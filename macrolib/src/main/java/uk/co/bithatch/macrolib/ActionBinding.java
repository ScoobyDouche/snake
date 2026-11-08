package uk.co.bithatch.macrolib;

import java.util.List;

import uk.co.bithatch.linuxio.EventCode;


/**
 * Created when an action is invoked and contains the keys that activated the
 * action (if any), the state they were in and the action ID.
 */
public class ActionBinding implements Comparable<ActionBinding> {

	private String action;
	private MacroDevice device;
	private KeySequence keySequence;

	/**
	 * Instantiates a new action binding.
	 *
	 * @param device the device
	 * @param action the action
	 * @param keys the keys
	 */
	public ActionBinding(MacroDevice device, String action, EventCode... keys) {
		this(device, action, new KeySequence(keys));
	}
	
	/**
	 * Instantiates a new action binding.
	 *
	 * @param device the device
	 * @param action the action
	 * @param keySequence the key sequence
	 */
	public ActionBinding(MacroDevice device, String action, KeySequence keySequence) {
		this.action = action;
		this.device = device;
		this.keySequence = keySequence;
	}
	
	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(ActionBinding o) {
		int i = Integer.valueOf(getKeys().size()).compareTo(o.getKeys().size());
		if (i == 0) {
			for (int j = 0; j < getKeys().size(); j++) {
				i = getKeys().get(j).compareTo(o.getKeys().get(j));
				if (i != 0)
					return i;
			}
		}
		if (i == 0)
			i = getState().compareTo(o.getState());
		return i;
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
	 * Gets the device.
	 *
	 * @return the device
	 */
	public MacroDevice getDevice() {
		return device;
	}

	/**
	 * Gets the keys.
	 *
	 * @return the keys
	 */
	public List<EventCode> getKeys() {
		return keySequence;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public KeyState getState() {
		return keySequence.getState();
	}
}
