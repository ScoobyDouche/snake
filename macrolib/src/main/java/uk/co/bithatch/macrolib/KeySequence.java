package uk.co.bithatch.macrolib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import uk.co.bithatch.linuxio.EventCode;


/**
 * The Class KeySequence.
 */
@SuppressWarnings("serial")
public class KeySequence extends ArrayList<EventCode> implements Comparable<KeySequence> {

	private KeyState state = KeyState.UP;

	/**
	 * Instantiates a new key sequence.
	 *
	 * @param codes the codes
	 */
	public KeySequence(EventCode... codes) {
		this(KeyState.DOWN, codes);
	}
	
	/**
	 * Instantiates a new key sequence.
	 *
	 * @param state the state
	 * @param codes the codes
	 */
	public KeySequence(KeyState state, Collection<EventCode> codes) {
		super(codes);
		setState(state);
	}

	/**
	 * Instantiates a new key sequence.
	 *
	 * @param state the state
	 * @param codes the codes
	 */
	public KeySequence(KeyState state, EventCode... codes) {
		super(Arrays.asList(codes));
		setState(state);
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(KeySequence o) {
		return strVal().compareTo(o.strVal());
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeySequence other = (KeySequence) obj;
		if (state != other.state)
			return false;
		return true;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public KeyState getState() {
		return state;
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	public void setState(KeyState state) {
		this.state = state;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return strVal();
	}
	
	protected String strVal() {
		return String.join("_", stream().map((k) -> k.name()).collect(Collectors.toList())) + "_" + state.name();
	}

}
