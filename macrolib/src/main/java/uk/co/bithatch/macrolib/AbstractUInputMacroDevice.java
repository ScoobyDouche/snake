package uk.co.bithatch.macrolib;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputController;
import uk.co.bithatch.linuxio.InputController.Callback;
import uk.co.bithatch.linuxio.InputDevice;


/**
 * The Class AbstractUInputMacroDevice.
 */
public abstract class AbstractUInputMacroDevice implements MacroDevice {

	private InputDevice device;

	/**
	 * Instantiates a new abstract U input macro device.
	 *
	 * @param devicePath the device path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public AbstractUInputMacroDevice(Path devicePath) throws IOException {
		device = new InputDevice(devicePath);
		device.grab();
	}

	/**
	 * Instantiates a new abstract U input macro device.
	 *
	 * @param devicePath the device path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public AbstractUInputMacroDevice(String devicePath) throws IOException {
		device = new InputDevice(devicePath);
		device.grab();
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		if (device == null || !device.isOpen())
			throw new IOException("Not open.");
		try {
			InputController.getInstance().remove(device);
		} finally {
			/* Release the file handle too, otherwise every unplug/replug leaks it */
			try {
				device.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public String getId() {
		return device.getName();
	}

	/**
	 * Gets the uid.
	 *
	 * @return the uid
	 */
	@Override
	public String getUID() {
		return String.format("%d:%d:%d:%d:%s", device.getBus(), device.getVendor(), device.getProduct(),
				device.getVersion(), device.getFile().getFileName());
	}

	/**
	 * Open.
	 *
	 * @param callback the callback
	 */
	@Override
	public void open(Callback callback) {
		InputController.getInstance().add(device, callback);
	}

	@Override
	public Collection<EventCode> getSupportedInputEvents() {
		return device.getCapabilities();
	}
}
