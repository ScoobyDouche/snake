package uk.co.bithatch.snake.ui;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class DPIControl extends ControlController {
	@FXML
	private Label dpiText;
	@FXML
	private Slider dpi;

	/* True while we move the slider to match the device, so we don't write it back */
	private boolean adjusting;
	private ScheduledFuture<?> poll;

	@Override
	protected void onSetControlDevice() {
		dpi.maxProperty().set(getDevice().getMaxDPI());
		dpi.valueProperty().set(getDevice().getDPI()[0]);
		dpi.snapToTicksProperty().set(true);
		dpi.majorTickUnitProperty().set(1000);
		dpi.minorTickCountProperty().set(100);
		dpi.valueProperty().addListener((e) -> {
			if (adjusting)
				return;
			short v = (short) ((short) ((float) dpi.valueProperty().get() / 100.0) * (short) 100);
			getDevice().setDPI(v, v);
			dpiText.textProperty().set(String.format("%d", getDevice().getDPI()[0]));
		});
		dpiText.textProperty().set(String.format("%d", getDevice().getDPI()[0]));

		/*
		 * The DPI buttons on the mouse change DPI in hardware and nothing tells us, so
		 * check the device once a second and follow it.
		 */
		poll = context.getSchedulerManager().get(Queue.DEVICE_IO).scheduleWithFixedDelay(() -> {
			try {
				int now = getDevice().getDPI()[0];
				Platform.runLater(() -> {
					if (dpi.isValueChanging() || (int) dpi.getValue() == now)
						return;
					adjusting = true;
					try {
						dpi.setValue(now);
						dpiText.textProperty().set(String.format("%d", now));
					} finally {
						adjusting = false;
					}
				});
			} catch (Exception e) {
				/* Device gone or busy; try again next time */
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	protected void onDeviceCleanUp() {
		if (poll != null) {
			poll.cancel(false);
			poll = null;
		}
	}
}
