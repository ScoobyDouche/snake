package uk.co.bithatch.macrolib;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputController.Callback;


/**
 * The Interface MacroDevice.
 */
public interface MacroDevice  extends Closeable {
	
	/**
	 * Adds the action.
	 *
	 * @param action the action
	 */
	default void addAction(ActionBinding action) {
		getActionKeys().put(action.getAction(), action);
	}

	/**
	 * Gets the action keys.
	 *
	 * @return the action keys
	 */
	Map<String, ActionBinding> getActionKeys();

	/**
	 * Gets the banks.
	 *
	 * @return the banks
	 */
	int getBanks();
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	String getId();
	
	/**
	 * Gets the joystick calibration.
	 *
	 * @return the joystick calibration
	 */
	int getJoystickCalibration();
	
	/**
	 * Gets the joystick mode.
	 *
	 * @return the joystick mode
	 */
	TargetType getJoystickMode();

	/**
	 * Gets the uid.
	 *
	 * @return the uid
	 */
	String getUID();
	
	/**
	 * Open.
	 *
	 * @param consumer the consumer
	 */
	void open(Callback consumer);

	/**
	 * Get the event codes this device supports (and so may be used to activate macros).
	 * 
	 * @return event codes
	 */
	Collection<EventCode> getSupportedInputEvents();
	
}
