package uk.co.bithatch.macrolib;

import java.io.Closeable;

import uk.co.bithatch.linuxio.InputController.Callback;

/**
 * The Class DeviceHandler.
 */
public abstract class DeviceHandler implements Callback, Closeable {

	protected EventConsumer callback;
	protected MacroDevice device;

	DeviceHandler(MacroDevice device, EventConsumer callback) {
		this.callback = callback;
		this.device = device;
	}
}
