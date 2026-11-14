package uk.co.bithatch.snake.ui;

import java.lang.System.Logger.Level;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.BackendException;
import uk.co.bithatch.snake.lib.Device;

/**
 * Full-window message in the new look, for when there is no device to show:
 * nothing plugged in, or the driver (openrazer) isn't running. Opens the
 * device page by itself as soon as a device turns up.
 */
public class Notice extends AbstractController implements BackendListener {
	final static System.Logger LOG = System.getLogger(Notice.class.getName());

	@FXML
	private BorderPane root;

	private boolean listening;

	@Override
	protected void onConfigure() throws Exception {
		root.getStyleClass().add("shell");
		showNoDevices();
	}

	/** Nothing plugged in (yet). */
	public void showNoDevices() {
		show("No devices found",
				"Plug in your Razer mouse or keyboard and it will show up here. If it's already plugged in, check that the openrazer daemon is running.",
				null);
		listen();
	}

	/** Something went wrong starting up. */
	public void showError(Throwable e) {
		Throwable root = e;
		while (root != null) {
			if (root instanceof BackendException) {
				show("Can't talk to the driver",
						"Snake needs openrazer (openrazer.github.io) to talk to Razer devices. Make sure it is installed and its daemon is running, then try again.",
						"Try again");
				return;
			}
			if (root.getCause() == null)
				break;
			root = root.getCause();
		}
		show("Something went wrong", (root == null ? "Unknown error" : root.getLocalizedMessage())
				+ "\n\nIf you've fixed it, try again.", "Try again");
	}

	private void show(String title, String text, String action) {
		var t = new Label(title);
		t.getStyleClass().add("pageTitle");
		var l = new Label(text);
		l.getStyleClass().add("lede");
		l.setWrapText(true);
		l.setMaxWidth(620);
		var box = new VBox(t, l);
		box.getStyleClass().add("page");
		if (action != null) {
			var b = new Button(action);
			b.getStyleClass().add("primary");
			b.setOnAction(e -> retry());
			box.getChildren().add(b);
		}
		root.setCenter(box);
	}

	private void retry() {
		try {
			var devs = context.getBackend().getDevices();
			if (devs.isEmpty())
				showNoDevices();
			else
				context.openDevice(devs.get(0));
		} catch (Exception e) {
			showError(e);
		}
	}

	private void listen() {
		if (listening)
			return;
		try {
			context.getBackend().addListener(this);
			listening = true;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Can't watch for devices.", e);
		}
	}

	@Override
	protected void onCleanUp() {
		if (listening) {
			context.getBackend().removeListener(this);
			listening = false;
		}
	}

	@Override
	public void deviceAdded(Device device) {
		Platform.runLater(() -> {
			try {
				context.openDevice(device);
			} catch (Exception e) {
				showError(e);
			}
		});
	}

	@Override
	public void deviceRemoved(Device device) {
	}
}
