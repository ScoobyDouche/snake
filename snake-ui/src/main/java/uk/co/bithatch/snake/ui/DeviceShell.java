package uk.co.bithatch.snake.ui;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.FileChooser;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.macrolib.ActionMacro;
import uk.co.bithatch.macrolib.CommandMacro;
import uk.co.bithatch.macrolib.KeySequence;
import uk.co.bithatch.macrolib.KeyState;
import uk.co.bithatch.macrolib.Macro;
import uk.co.bithatch.macrolib.MacroBank;
import uk.co.bithatch.macrolib.MacroDevice;
import uk.co.bithatch.macrolib.NoopMacro;
import uk.co.bithatch.macrolib.RepeatMode;
import uk.co.bithatch.macrolib.ScriptMacro;
import uk.co.bithatch.macrolib.SimpleMacro;
import uk.co.bithatch.macrolib.UInputMacro;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.lib.effects.Wave;
import uk.co.bithatch.snake.ui.effects.AbstractBackendEffectHandler;
import uk.co.bithatch.snake.ui.effects.AudioEffectHandler;
import uk.co.bithatch.snake.ui.effects.AudioEffectMode;
import uk.co.bithatch.snake.ui.effects.BlobbyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.BurstyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.ExpandAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.LineyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.PeaksAudioEffectMode;
import uk.co.bithatch.snake.lib.OnboardProfiles;
import uk.co.bithatch.snake.lib.OnboardProfiles.Action;
import uk.co.bithatch.snake.lib.OnboardProfiles.DpiStages;
import uk.co.bithatch.snake.lib.OnboardProfiles.Lighting;
import uk.co.bithatch.snake.ui.OnboardText.HidKeys;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.ui.effects.EffectManager;
import uk.co.bithatch.snake.ui.effects.SoftwareEffects;
import uk.co.bithatch.snake.widgets.Direction;

/**
 * The device page: a menu on the left (Buttons, Performance, Lighting, Macros,
 * Settings) and the chosen page on the right, with the profile picker on top.
 * Profiles stored on the mouse are edited directly when the mouse supports it.
 */
public class DeviceShell extends AbstractDeviceController {

	final static Logger LOG = System.getLogger(DeviceShell.class.getName());

	/* ---- photo geometry for known mice (openrazer's 500x500 product photo) ---- */

	/** Part of the photo, in photo pixels. */
	record Crop(double x, double y, double w, double h) {
	}

	/** Where a button is: which crop it's in and its centre in photo pixels. */
	record Spot(String crop, double x, double y) {
	}

	private static final double SCALE = 1.1;
	private static final Map<String, Crop> TRINITY_CROPS = Map.of("mouse", new Crop(246, 18, 208, 460),
			"2-button plate", new Crop(52, 100, 92, 330), "7-button plate", new Crop(146, 100, 98, 330));
	private static final Map<Integer, Spot> TRINITY_SPOTS = new LinkedHashMap<>();
	static {
		var m = TRINITY_SPOTS;
		m.put(0x01, new Spot("mouse", 310, 120));
		m.put(0x02, new Spot("mouse", 403, 127));
		m.put(0x03, new Spot("mouse", 375, 160));
		m.put(0x34, new Spot("mouse", 375, 150));
		m.put(0x35, new Spot("mouse", 375, 172));
		m.put(0x09, new Spot("mouse", 361, 160));
		m.put(0x0a, new Spot("mouse", 389, 160));
		m.put(0x0b, new Spot("mouse", 386, 210));
		m.put(0x0c, new Spot("mouse", 388, 237));
		m.put(0x04, new Spot("2-button plate", 105, 227));
		m.put(0x05, new Spot("2-button plate", 105, 267));
		double[][] seven = { { 174, 223 }, { 197, 232 }, { 209, 263 }, { 157, 241 }, { 156, 275 }, { 170, 297 },
				{ 196, 292 } };
		for (int i = 0; i < 7; i++)
			m.put(0x50 + i, new Spot("7-button plate", seven[i][0], seven[i][1]));
		double[][] twelve = { { 264, 234 }, { 279, 230 }, { 296, 225 }, { 265, 259 }, { 281, 255 }, { 301, 250 },
				{ 266, 284 }, { 282, 279 }, { 304, 274 }, { 267, 308 }, { 284, 302 }, { 303, 297 } };
		for (int i = 0; i < 12; i++)
			m.put(0x40 + i, new Spot("mouse", twelve[i][0], twelve[i][1]));
	}

	@FXML
	private BorderPane root;

	private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
		var t = new Thread(r, "DeviceShellIO");
		t.setDaemon(true);
		return t;
	});
	private final ScheduledExecutorService poll = Executors.newSingleThreadScheduledExecutor(r -> {
		var t = new Thread(r, "DeviceShellPoll");
		t.setDaemon(true);
		return t;
	});
	private ScheduledFuture<?> pollTask;

	private OnboardProfiles onboard;
	private List<Integer> slots = List.of();
	private int profile = 1;
	private int activeProfile = -1;
	private boolean backedUp;
	private boolean loaded;

	private final Map<Integer, Action> actions = new LinkedHashMap<>();
	private DpiStages stages;
	private final Map<Integer, Lighting> lights = new LinkedHashMap<>();

	private String page = "buttons";
	private final VBox nav = new VBox();
	private final Map<String, Label> navItems = new LinkedHashMap<>();
	private final BorderPane content = new BorderPane();
	private final VBox editor = new VBox(10);
	private final Label status = new Label();
	private final Label footerPlate = new Label();
	private final ComboBox<Integer> profilePick = new ComboBox<>();
	private boolean updatingPick;

	/* buttons page state */
	private String plate;
	private Integer editing;
	private Pane lines;
	private StackPane stage;
	private final Map<Integer, Button> slotButtons = new LinkedHashMap<>();
	private final Map<String, ImageView> cropViews = new LinkedHashMap<>();
	private Image photo;

	/* ---- lifecycle ---- */

	/** Page to show first (e.g. "devices" when there are several devices). */
	public void setInitialPage(String page) {
		this.page = page;
	}

	@Override
	protected void onConfigure() throws Exception {
		root.getStyleClass().add("shell");
	}

	@Override
	protected void onSetDevice() throws Exception {
		Device dev = getDevice();
		photo = new Image(context.getDefaultImage(dev.getType(), context.getCache().getCachedImage(dev.getImage())),
				false);
		buildFrame();
		showPage(page);
		io.execute(this::openOnboard);
		if (!watchingBackend) {
			context.getBackend().addListener(unplugWatch);
			watchingBackend = true;
		}
	}

	private boolean watchingBackend;

	/** When this device goes away, show another one or the "no devices" notice. */
	private final uk.co.bithatch.snake.lib.Backend.BackendListener unplugWatch = new uk.co.bithatch.snake.lib.Backend.BackendListener() {
		@Override
		public void deviceAdded(Device device) {
		}

		@Override
		public void deviceRemoved(Device device) {
			if (!device.equals(getDevice()))
				return;
			Platform.runLater(() -> {
				try {
					var others = context.getBackend().getDevices().stream().filter(d -> !d.equals(device)).toList();
					if (!others.isEmpty())
						context.openDevice(others.get(0));
					else {
						context.clearScreens();
						context.push(Notice.class, Direction.FADE);
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Couldn't leave the unplugged device.", e);
				}
			});
		}
	};

	@Override
	protected void onDeviceCleanUp() {
		if (watchingBackend) {
			context.getBackend().removeListener(unplugWatch);
			watchingBackend = false;
		}
		if (pollTask != null)
			pollTask.cancel(false);
		poll.shutdownNow();
		io.shutdown();
	}

	/* ---- frame: menu, header, footer ---- */

	private void buildFrame() throws Exception {
		nav.getStyleClass().add("shellNav");
		nav.getChildren().clear();
		navItems.clear();
		nav.setMaxHeight(Double.MAX_VALUE);
		Object group = null;
		nav.getChildren().addAll(navButton("Devices", "devices", group), separator());
		for (var p : List.of(new String[] { "buttons", "Buttons" }, new String[] { "performance", "Performance" },
				new String[] { "lighting", "Lighting" }, new String[] { "macros", "Macros" }))
			nav.getChildren().add(navButton(p[1], p[0], group));
		nav.getChildren().addAll(separator(), navButton("Settings", "settings", group));

		var name = new Label(getDevice().getName());
		deviceName = name;
		name.getStyleClass().add("shellDevice");
		var spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		var pickLabel = new Label("Profile");
		pickLabel.getStyleClass().add("muted");
		profilePick.setCellFactory(l -> new ProfileCell());
		profilePick.setButtonCell(new ProfileCell());
		profilePick.setOnAction(e -> {
			if (updatingPick || profilePick.getValue() == null || profilePick.getValue() == profile)
				return;
			profile = profilePick.getValue();
			editing = null;
			reloadProfile();
		});
		var pick = new HBox(10, pickLabel, profilePick);
		pick.setAlignment(Pos.CENTER_RIGHT);
		pick.visibleProperty().set(false);
		pick.managedProperty().bind(pick.visibleProperty());
		pickBox = pick;
		var header = new HBox(16, name, spacer, pick);
		header.setAlignment(Pos.CENTER_LEFT);
		header.getStyleClass().add("shellHeader");

		var footer = new HBox(26, footerPlate, status);
		footer.setAlignment(Pos.CENTER_LEFT);
		footer.getStyleClass().add("shellFooter");
		footerPlate.managedProperty().bind(footerPlate.visibleProperty());

		editor.getStyleClass().add("shellEditor");
		editor.setVisible(false);
		editor.managedProperty().bind(editor.visibleProperty());

		var main = new BorderPane();
		main.setTop(header);
		main.setCenter(content);
		main.setRight(editor);
		main.setBottom(footer);
		root.setLeft(nav);
		root.setCenter(main);
	}

	private HBox pickBox;
	private Label deviceName;

	private Label navButton(String text, String id, Object unused) {
		var b = new Label(text);
		b.getStyleClass().add("navItem");
		b.setMaxWidth(Double.MAX_VALUE);
		if (id != null) {
			navItems.put(id, b);
			b.setOnMouseClicked(e -> showPage(id));
		}
		return b;
	}

	private Region separator() {
		var r = new Region();
		r.getStyleClass().add("navSeparator");
		return r;
	}

	private class ProfileCell extends ListCell<Integer> {
		@Override
		protected void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null)
				setText(null);
			else
				setText(profileName(item) + (item == activeProfile ? " (on mouse now)" : ""));
		}
	}

	private String profileName(int slot) {
		int i = slots.indexOf(slot);
		return "Profile " + (i < 0 ? slot : i + 1);
	}

	private void showPage(String id) {
		page = id;
		navItems.forEach((k, b) -> {
			b.getStyleClass().remove("selected");
			if (k.equals(id))
				b.getStyleClass().add("selected");
		});
		closeEditor();
		pickBox.setVisible(onboard != null && !List.of("settings", "macros", "devices").contains(id));
		deviceName.setVisible(!id.equals("devices"));
		footerPlate.setVisible(id.equals("buttons") && plate != null);
		Node n;
		switch (id) {
		case "performance":
			n = performancePage();
			break;
		case "lighting":
			n = lightingPage();
			break;
		case "macros":
			n = macrosPage();
			break;
		case "settings":
			n = settingsPage();
			break;
		case "devices":
			n = devicesPage();
			break;
		default:
			n = buttonsPage();
		}
		var sp = new ScrollPane(n);
		sp.setFitToWidth(true);
		sp.getStyleClass().add("shellScroll");
		content.setCenter(sp);
	}

	private VBox pageBox(String title, String lede) {
		var t = new Label(title);
		t.getStyleClass().add("pageTitle");
		var box = new VBox(t);
		box.getStyleClass().add("page");
		if (lede != null) {
			var l = new Label(lede);
			l.getStyleClass().add("lede");
			l.setWrapText(true);
			l.setMaxWidth(620);
			box.getChildren().add(l);
		}
		return box;
	}

	private Label sectionTitle(String text) {
		var l = new Label(text);
		l.getStyleClass().add("sectionTitle");
		return l;
	}

	private Label hint(String text) {
		var l = new Label(text);
		l.getStyleClass().add("hint");
		l.setWrapText(true);
		l.setMaxWidth(620);
		return l;
	}

	/* ---- onboard profiles: loading and polling ---- */

	/** Run mouse I/O with Snake's software effects paused, so replies don't get lost. */
	private <T> T withMouse(IOCall<T> call) throws Exception {
		SoftwareEffects.pause();
		try {
			Thread.sleep(60);
			return call.call();
		} finally {
			SoftwareEffects.resume();
		}
	}

	interface IOCall<T> {
		T call() throws Exception;
	}

	private void openOnboard() {
		try {
			OnboardProfiles ob = getDevice().getOnboardProfiles();
			if (ob == null) {
				String err = getDevice().getOnboardProfilesError();
				Platform.runLater(() -> {
					onboardError = err;
					loaded = true;
					showPage(page);
				});
				return;
			}
			ob.setExperimentalWrites(context.getPreferences(getDevice()).getBoolean("onboardExperimental", false));
			var sl = withMouse(ob::getProfiles);
			int act = withMouse(ob::getActiveProfile);
			onboard = ob;
			slots = sl;
			activeProfile = act;
			profile = slots.contains(act) ? act : slots.get(0);
			loadProfileData();
			pollTask = poll.scheduleWithFixedDelay(this::pollActive, 3, 3, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to open onboard profiles.", e);
			Platform.runLater(() -> {
				onboardError = "Could not read the mouse: " + e.getMessage();
				loaded = true;
				showPage(page);
			});
		}
	}

	private String onboardError;

	private void reloadProfile() {
		io.execute(this::loadProfileData);
	}

	private void loadProfileData() {
		try {
			var acts = new LinkedHashMap<Integer, Action>();
			var ls = new LinkedHashMap<Integer, Lighting>();
			DpiStages st = withMouse(() -> {
				for (int b : onboard.getButtonIds())
					acts.put(b, onboard.getButton(profile, b));
				for (int z : onboard.getLedZones())
					ls.put(z, onboard.getLighting(profile, z));
				return onboard.getDpiStages(profile);
			});
			Platform.runLater(() -> {
				actions.clear();
				actions.putAll(acts);
				lights.clear();
				lights.putAll(ls);
				stages = st;
				loaded = true;
				updatePick();
				Integer keep = editing;
				showPage(page);
				if (keep != null && page.equals("buttons"))
					showEditor(keep);
			});
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to read profile.", e);
			Platform.runLater(() -> setStatus("Could not read " + profileName(profile) + ": " + e.getMessage(), true));
		}
	}

	private void pollActive() {
		try {
			int a = withMouse(onboard::getActiveProfile);
			if (a != activeProfile) {
				activeProfile = a;
				if (editing == null && slots.contains(a)) {
					profile = a;
					loadProfileData();
				} else
					Platform.runLater(this::updatePick);
			}
		} catch (Exception e) {
			LOG.log(Level.DEBUG, "Poll failed.", e);
		}
	}

	private void updatePick() {
		updatingPick = true;
		try {
			profilePick.getItems().setAll(slots);
			profilePick.setValue(profile);
			/* force the button cell to refresh the "(on mouse now)" text */
			profilePick.setButtonCell(new ProfileCell());
		} finally {
			updatingPick = false;
		}
		pickBox.setVisible(onboard != null && !List.of("settings", "macros", "devices").contains(page));
		int pos = slots.indexOf(activeProfile);
		setStatus(pos >= 0 ? "Mouse is on " + profileName(activeProfile)
				+ ". Press the button under the mouse to switch." : "", false);
	}

	private void setStatus(String text, boolean error) {
		status.setText(text);
		status.getStyleClass().remove("error");
		if (error)
			status.getStyleClass().add("error");
	}

	/** Messages for pages that need the onboard profiles. */
	private Node onboardMissing(VBox box) {
		if (!loaded) {
			box.getChildren().add(hint("Reading the profiles from the mouse..."));
			return box;
		}
		if (onboardError != null && onboardError.contains("udev")) {
			box.getChildren().add(hint("Snake needs permission to talk to this mouse directly. This adds a small "
					+ "system rule that gives you (the person logged in) access to this mouse only."));
			var enable = new Button("Allow access");
			enable.getStyleClass().add("primary");
			enable.setOnAction(e -> installRule(enable));
			box.getChildren().add(enable);
		} else {
			box.getChildren().add(hint(onboardError == null
					? "This device doesn't store profiles that Snake can read."
					: onboardError));
		}
		return box;
	}

	private void installRule(Button button) {
		button.setDisable(true);
		io.execute(() -> {
			try {
				int[] ids = getDevice().getUsbIds();
				if (ids == null || ids.length < 2)
					throw new Exception("Unknown USB id");
				String pid = String.format("%04x", ids[1]);
				Path tmp = Files.createTempFile("snake-razer", ".rules");
				/* only this mouse, and only for the user logged in at the seat */
				Files.writeString(tmp, String.format(
						"# Installed by Snake: onboard profile access for %s (1532:%s), logged-in user only%n"
								+ "SUBSYSTEM==\"hidraw\", ATTRS{idVendor}==\"1532\", ATTRS{idProduct}==\"%s\", TAG+=\"uaccess\"%n",
						getDevice().getName().replaceAll("[\\r\\n]", " "), pid, pid));
				var p = new ProcessBuilder("pkexec", "sh", "-c",
						"install -m 644 \"$1\" \"/etc/udev/rules.d/70-snake-razer-$2.rules\" && udevadm trigger --subsystem-match=hidraw && udevadm settle",
						"sh", tmp.toString(), pid).inheritIO().start();
				int rc = p.waitFor();
				Files.deleteIfExists(tmp);
				if (rc != 0)
					throw new Exception("Access wasn't allowed (the password prompt was closed or failed).");
				Thread.sleep(1000);
				Platform.runLater(() -> {
					onboardError = null;
					loaded = false;
				});
				openOnboard();
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to install rule.", e);
				Platform.runLater(() -> {
					button.setDisable(false);
					setStatus(e.getMessage(), true);
				});
			}
		});
	}

	/* ---- Buttons page ---- */

	private Node buttonsPage() {
		var box = pageBox("Buttons", onboard == null ? null
				: "Click a button to change what it does. Changes are saved on the mouse, so they work on any computer, even without Snake.");
		slotButtons.clear();
		cropViews.clear();
		if (onboard == null || actions.isEmpty())
			return onboardMissing(box);

		var groups = new LinkedHashMap<String, List<Integer>>();
		for (int b : onboard.getButtonIds())
			groups.computeIfAbsent(onboard.getButtonGroup(b), k -> new ArrayList<>()).add(b);
		var plates = groups.keySet().stream().filter(g -> g.contains("plate")).toList();
		if (!plates.isEmpty()) {
			var prefs = context.getPreferences(getDevice());
			if (plate == null || !plates.contains(plate))
				plate = prefs.get("onboardPlate", plates.get(0));
			if (!plates.contains(plate))
				plate = plates.get(0);
			var seg = new HBox(6);
			seg.getStyleClass().add("segments");
			var tg = new ToggleGroup();
			for (String p : plates) {
				var t = new ToggleButton(p.replace("-button plate", " buttons"));
				t.getStyleClass().add("segment");
				t.setToggleGroup(tg);
				t.setSelected(p.equals(plate));
				t.setOnAction(e -> {
					plate = p;
					prefs.put("onboardPlate", p);
					editing = null;
					showPage("buttons");
				});
				seg.getChildren().add(t);
			}
			box.getChildren().add(seg);
			footerPlate.setText("Side plate: " + plate.replace("-button plate", " buttons"));
			footerPlate.setVisible(true);
		}

		boolean pictured = onboard.getModelName() != null && onboard.getModelName().contains("Trinity")
				&& photo.getWidth() == 500 && photo.getHeight() == 500;

		/* side plate buttons on the left, everything else on the right */
		List<Integer> side = plate == null ? List.of() : groups.get(plate);
		var left = new Pane();
		Node leftCol;
		if (side.size() > 7) {
			var g = new GridPane();
			g.getStyleClass().add("slotGrid");
			for (int i = 0; i < side.size(); i++)
				g.add(slot(side.get(i), true), i % 3, i / 3);
			leftCol = g;
		} else {
			var v = new VBox(8);
			for (int b : side)
				v.getChildren().add(slot(b, false));
			leftCol = v;
		}
		var right = new VBox(8);
		for (var e : groups.entrySet()) {
			if (plates.contains(e.getKey()))
				continue;
			for (int b : e.getValue())
				right.getChildren().add(slot(b, false));
		}

		var pics = new HBox(30);
		pics.setAlignment(Pos.CENTER);
		if (pictured) {
			if (TRINITY_CROPS.containsKey(plate))
				pics.getChildren().add(cropView(plate));
			pics.getChildren().add(cropView("mouse"));
		} else {
			var iv = new ImageView(photo);
			iv.setPreserveRatio(true);
			iv.setFitWidth(320);
			pics.getChildren().add(iv);
		}

		var row = new HBox(70, leftCol, pics, right);
		row.setAlignment(Pos.CENTER);
		row.setFillHeight(false);
		lines = new Pane();
		lines.setMouseTransparent(true);
		lines.setManaged(false);
		stage = new StackPane(row, lines);
		stage.getStyleClass().add("buttonStage");
		stage.layoutBoundsProperty().addListener((o, a, b) -> drawLines());
		box.getChildren().add(stage);
		left.getChildren().clear();
		return box;
	}

	private ImageView cropView(String name) {
		Crop c = TRINITY_CROPS.get(name);
		var iv = new ImageView(photo);
		iv.setViewport(new Rectangle2D(c.x(), c.y(), c.w(), c.h()));
		iv.setFitWidth(c.w() * SCALE);
		iv.setFitHeight(c.h() * SCALE);
		iv.setSmooth(true);
		cropViews.put(name, iv);
		return iv;
	}

	private Button slot(int id, boolean compact) {
		String name = onboard.getButtonName(id);
		if (onboard.getButtonGroup(id).equals("Bottom"))
			name = "Underside button";
		var t = new Label(name);
		t.getStyleClass().add("slotName");
		var a = new Label(OnboardText.describe(actions.get(id)));
		a.getStyleClass().add("slotAction");
		var b = new Button();
		b.setGraphic(new VBox(1, t, a));
		b.getStyleClass().add("slot");
		if (compact)
			b.getStyleClass().add("compact");
		if (editing != null && editing == id)
			b.getStyleClass().add("selected");
		b.setOnAction(e -> showEditor(id));
		b.hoverProperty().addListener((o, x, y) -> drawLines());
		slotButtons.put(id, b);
		return b;
	}

	/** Draw the line for the hovered or selected button only. */
	private void drawLines() {
		if (lines == null || stage == null)
			return;
		lines.getChildren().clear();
		if (cropViews.isEmpty())
			return;
		for (var e : slotButtons.entrySet()) {
			Button b = e.getValue();
			boolean hot = b.isHover() || (editing != null && editing.equals(e.getKey()));
			if (!hot)
				continue;
			Spot s = TRINITY_SPOTS.get(e.getKey());
			if (s == null)
				continue;
			ImageView iv = cropViews.get(s.crop());
			Crop c = TRINITY_CROPS.get(s.crop());
			if (iv == null || c == null)
				continue;
			Bounds ib = stage.sceneToLocal(iv.localToScene(iv.getBoundsInLocal()));
			Bounds bb = stage.sceneToLocal(b.localToScene(b.getBoundsInLocal()));
			double px = ib.getMinX() + (s.x() - c.x()) * SCALE;
			double py = ib.getMinY() + (s.y() - c.y()) * SCALE;
			boolean leftSide = bb.getCenterX() < px;
			double x1 = leftSide ? bb.getMaxX() : bb.getMinX();
			double y1 = bb.getCenterY();
			double elbow = leftSide ? x1 + 18 : x1 - 18;
			var pl = new Polyline(x1, y1, elbow, y1, px, py);
			pl.getStyleClass().add("calloutLine");
			var dot = new Circle(px, py, 6);
			dot.getStyleClass().add("calloutDot");
			lines.getChildren().addAll(pl, dot);
		}
	}

	/* ---- button editor (right-hand panel) ---- */

	private void closeEditor() {
		editing = null;
		editor.setVisible(false);
		slotButtons.values().forEach(b -> b.getStyleClass().remove("selected"));
		macroRows.forEach(b -> b.getStyleClass().remove("selected"));
		drawLines();
	}

	private void showEditor(int id) {
		editing = id;
		slotButtons.forEach((k, b) -> {
			b.getStyleClass().remove("selected");
			if (k == id)
				b.getStyleClass().add("selected");
		});
		drawLines();
		Action cur = actions.get(id);
		var t = new Label(onboard.getButtonGroup(id).equals("Bottom") ? "Underside button"
				: onboard.getButtonGroup(id).contains("plate") ? onboard.getButtonGroup(id) + ", " + onboard.getButtonName(id)
						: onboard.getButtonName(id));
		t.getStyleClass().add("sectionTitle");
		t.setWrapText(true);
		var now = hint("Now: " + OnboardText.describe(cur));
		editor.getChildren().setAll(t, now);
		editor.setVisible(true);
		if (id == 0x01) {
			editor.getChildren().add(hint("Left click can't be changed, so the mouse always stays usable."));
			editor.getChildren().add(cancelButton("Close"));
			return;
		}
		if (!onboard.isWritable())
			editor.getChildren().add(hint("Saving to this mouse is off. Turn on experimental saving in Settings."));

		var kinds = new ArrayList<String>(List.of("Keyboard key", "Mouse button", "DPI up", "DPI down",
				"Next profile", "Turn off"));
		boolean keep = cur != null && (cur.type() == Action.MACRO || cur.type() == Action.MACRO_REPEAT
				|| cur.type() == Action.SCROLL);
		if (keep)
			kinds.add(0, "Keep: " + OnboardText.describe(cur));
		var grid = new GridPane();
		grid.getStyleClass().add("choiceGrid");
		var tg = new ToggleGroup();
		var detail = new VBox(8);
		Action[] chosen = { cur };
		for (int i = 0; i < kinds.size(); i++) {
			String k = kinds.get(i);
			var tb = new ToggleButton(k);
			tb.getStyleClass().add("segment");
			tb.setMaxWidth(Double.MAX_VALUE);
			tb.setToggleGroup(tg);
			tb.setOnAction(e -> {
				tb.setSelected(true);
				chooseKind(k, cur, chosen, detail);
			});
			if (keep && i == 0)
				GridPane.setColumnSpan(tb, 2);
			int idx = keep ? (i == 0 ? 0 : i + 1) : i;
			grid.add(tb, idx % 2, idx / 2);
			if (keep ? i == 0 : k.equals(initialKind(cur)))
				tb.setSelected(true);
		}
		var sel = (ToggleButton) tg.getSelectedToggle();
		if (sel != null)
			chooseKind(sel.getText(), cur, chosen, detail);

		var save = new Button("Save to mouse");
		save.getStyleClass().add("primary");
		save.setDisable(!onboard.isWritable());
		save.setDefaultButton(true);
		save.setOnAction(e -> {
			Action a = chosen[0];
			if (a == null) {
				setStatus("Press a key first.", true);
				return;
			}
			save.setDisable(true);
			int p = profile;
			write("the button", () -> onboard.setButton(p, id, a), () -> {
				actions.put(id, a);
				closeEditor();
				showPage("buttons");
			}, () -> save.setDisable(false));
		});
		var label = new Label("Make it");
		label.getStyleClass().add("fieldLabel");
		var acts = new HBox(10, save, cancelButton("Cancel"));
		editor.getChildren().addAll(label, grid, detail, acts);
	}

	private void chooseKind(String k, Action cur, Action[] chosen, VBox detail) {
		detail.getChildren().clear();
		if (k.startsWith("Keep")) {
			chosen[0] = cur;
		} else if (k.equals("Keyboard key")) {
			boolean wasKey = cur != null && cur.type() == Action.KEY;
			chosen[0] = wasKey ? cur : null;
			var cap = new Button(wasKey ? OnboardText.describe(cur) : "Click here, then press a key");
			cap.getStyleClass().add("keyCapture");
			cap.setMaxWidth(Double.MAX_VALUE);
			cap.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
				ke.consume();
				if (isModifier(ke.getCode()))
					return;
				int usage = HidKeys.usage(ke.getCode());
				if (usage == 0) {
					cap.setText("That key can't be used, try another");
					return;
				}
				int mods = (ke.isControlDown() ? 1 : 0) | (ke.isShiftDown() ? 2 : 0) | (ke.isAltDown() ? 4 : 0)
						| (ke.isMetaDown() ? 8 : 0);
				chosen[0] = Action.key(mods, usage);
				cap.setText(OnboardText.describe(chosen[0]));
			});
			var l = new Label("Key (hold Ctrl, Shift or Alt too if you want them)");
			l.getStyleClass().add("fieldLabel");
			l.setWrapText(true);
			detail.getChildren().addAll(l, cap);
			Platform.runLater(cap::requestFocus);
		} else if (k.equals("Mouse button")) {
			var mb = new ComboBox<String>();
			mb.getItems().addAll(OnboardText.MOUSE_NAMES.values());
			int curBtn = cur != null && cur.type() == Action.MOUSE && cur.params().length > 0 ? cur.params()[0] : 4;
			mb.setValue(OnboardText.MOUSE_NAMES.getOrDefault(curBtn, OnboardText.MOUSE_NAMES.get(4)));
			Runnable upd = () -> OnboardText.MOUSE_NAMES.forEach((n, v) -> {
				if (v.equals(mb.getValue()))
					chosen[0] = Action.mouse(n);
			});
			mb.setOnAction(x -> upd.run());
			upd.run();
			mb.setMaxWidth(Double.MAX_VALUE);
			detail.getChildren().add(mb);
		} else if (k.equals("DPI up")) {
			chosen[0] = new Action(Action.DPI, new byte[] { 1 });
		} else if (k.equals("DPI down")) {
			chosen[0] = new Action(Action.DPI, new byte[] { 2 });
		} else if (k.equals("Next profile")) {
			chosen[0] = new Action(Action.PROFILE, new byte[] { 1 });
		} else {
			chosen[0] = Action.disabled();
		}
	}

	private Button cancelButton(String text) {
		var c = new Button(text);
		c.getStyleClass().add("ghost");
		c.setOnAction(e -> closeEditor());
		return c;
	}

	private static String initialKind(Action a) {
		if (a == null)
			return "Keyboard key";
		return switch (a.type()) {
		case Action.KEY -> "Keyboard key";
		case Action.MOUSE -> "Mouse button";
		case Action.DPI -> a.params().length > 0 && a.params()[0] == 2 ? "DPI down" : "DPI up";
		case Action.PROFILE -> "Next profile";
		case Action.DISABLED -> "Turn off";
		default -> "Keyboard key";
		};
	}

	private static boolean isModifier(KeyCode c) {
		return c == KeyCode.CONTROL || c == KeyCode.SHIFT || c == KeyCode.ALT || c == KeyCode.META
				|| c == KeyCode.WINDOWS || c == KeyCode.ALT_GRAPH || c == KeyCode.COMMAND;
	}

	/* ---- Performance page ---- */

	private Node performancePage() {
		Device dev = getDevice();
		var box = pageBox("Performance", onboard != null && stages != null
				? "The DPI buttons on top of the mouse step through these speeds. The highlighted one is used when you switch to this profile."
				: null);
		if (onboard != null && stages != null) {
			box.getChildren().add(sectionTitle("DPI stages"));
			var row = new HBox(14);
			var tg = new ToggleGroup();
			var spinners = new ArrayList<Spinner<Integer>>();
			var starts = new ArrayList<ToggleButton>();
			for (int i = 0; i < stages.dpi().length; i++) {
				var s = new Spinner<Integer>(100, dev.getMaxDPI() > 0 ? dev.getMaxDPI() : 16000, stages.dpi()[i], 50);
				s.setEditable(true);
				s.getStyleClass().add("stageSpinner");
				var start = new ToggleButton(i == stages.active() ? "Start" : "Stage " + (i + 1));
				start.getStyleClass().add("stageLabel");
				start.setToggleGroup(tg);
				start.setSelected(i == stages.active());
				int fi = i;
				start.selectedProperty().addListener((o, a, b) -> start.setText(b ? "Start" : "Stage " + (fi + 1)));
				var tile = new VBox(6, s, start);
				tile.getStyleClass().add("stageTile");
				start.selectedProperty().addListener((o, a, b) -> {
					if (b)
						tile.getStyleClass().add("on");
					else
						tile.getStyleClass().remove("on");
				});
				if (i == stages.active())
					tile.getStyleClass().add("on");
				spinners.add(s);
				starts.add(start);
				row.getChildren().add(tile);
			}
			box.getChildren().add(row);
			box.getChildren().add(hint("Type a speed or use the arrows. Click a stage's name to make it the starting one."));
			var save = new Button("Save to mouse");
			save.getStyleClass().add("primary");
			save.setDisable(!onboard.isWritable());
			save.setOnAction(e -> {
				int[] d = spinners.stream().mapToInt(s -> {
					s.increment(0); /* commit typed text */
					return s.getValue();
				}).toArray();
				int act = 0;
				for (int i = 0; i < starts.size(); i++)
					if (starts.get(i).isSelected())
						act = i;
				int p = profile, fa = act;
				write("the DPI stages", () -> onboard.setDpiStages(p, new DpiStages(fa, d)),
						() -> stages = new DpiStages(fa, d), null);
			});
			box.getChildren().add(save);
		} else {
			box.getChildren().add(sectionTitle("Speed (DPI)"));
			int[] dpi = dev.getDPI();
			var sl = new Slider(100, dev.getMaxDPI() > 0 ? dev.getMaxDPI() : 16000, dpi != null ? dpi[0] : 800);
			sl.setMajorTickUnit(100);
			sl.setSnapToTicks(true);
			sl.setMaxWidth(520);
			var v = new Label();
			v.textProperty().bind(sl.valueProperty().asString("%.0f DPI"));
			sl.valueChangingProperty().addListener((o, a, b) -> {
				if (!b)
					dev.setDPI((short) sl.getValue(), (short) sl.getValue());
			});
			box.getChildren().addAll(new HBox(14, sl, v));
		}

		var sep = new Region();
		sep.getStyleClass().add("divider");
		box.getChildren().addAll(sep, sectionTitle("Polling rate"),
				hint("How often the mouse reports its position. Higher is smoother; 1000 Hz suits most people."));
		var pr = new HBox(6);
		pr.getStyleClass().add("segments");
		var tg = new ToggleGroup();
		int cur = dev.getPollRate();
		for (int r : new int[] { 125, 500, 1000 }) {
			var t = new ToggleButton(r + " Hz");
			t.getStyleClass().add("segment");
			t.setToggleGroup(tg);
			t.setSelected(r == cur);
			t.setOnAction(e -> {
				t.setSelected(true);
				context.getSchedulerManager().get(SchedulerManager.Queue.DEVICE_IO).execute(() -> dev.setPollRate(r));
				setStatus("Polling rate set to " + r + " Hz.", false);
			});
			pr.getChildren().add(t);
		}
		box.getChildren().add(pr);
		return box;
	}

	/* ---- Lighting page ---- */

	private static final Map<Integer, String> EFFECTS = new LinkedHashMap<>();
	static {
		EFFECTS.put(Lighting.STATIC, "Static colour");
		EFFECTS.put(Lighting.SPECTRUM, "Spectrum");
		EFFECTS.put(Lighting.OFF, "Off");
	}
	private static final Color[] SWATCHES = { Color.web("#44d62c"), Color.web("#ff2a2a"), Color.web("#2a7bff"),
			Color.web("#ffffff"), Color.web("#ffb000"), Color.web("#b04cff") };

	private Node lightingPage() {
		Device dev = getDevice();
		var box = pageBox("Lighting", onboard != null ? "Saved with " + profileName(profile) + " on the mouse." : null);
		if (onboard != null && !lights.isEmpty()) {
			var grid = new GridPane();
			grid.getStyleClass().add("zoneGrid");
			int row = 0;
			var editors = new LinkedHashMap<Integer, Object[]>();
			for (var e : lights.entrySet()) {
				int zone = e.getKey();
				Lighting l = e.getValue();
				var name = new Label(onboard.getLedZoneName(zone));
				var eff = new ComboBox<String>();
				eff.getItems().addAll(EFFECTS.values());
				boolean known = EFFECTS.containsKey(l.effect());
				if (!known)
					eff.getItems().add(0, "Keep current");
				eff.setValue(known ? EFFECTS.get(l.effect()) : "Keep current");
				int[] c0 = l.colors().isEmpty() ? new int[] { 0x44, 0xd6, 0x2c } : fromLed(l.colors().get(0));
				var picker = new ColorPicker(Color.rgb(c0[0], c0[1], c0[2]));
				picker.getStyleClass().add("button");
				var sw = new HBox(6);
				for (Color c : SWATCHES) {
					var b = new Button();
					b.getStyleClass().add("swatch");
					b.setStyle("-fx-background-color: " + toHex(c) + ";");
					b.setOnAction(x -> picker.setValue(c));
					sw.getChildren().add(b);
				}
				var colours = new HBox(10, sw, picker);
				colours.setAlignment(Pos.CENTER_LEFT);
				colours.visibleProperty().bind(eff.valueProperty().isEqualTo(EFFECTS.get(Lighting.STATIC)));
				grid.addRow(row++, name, eff, colours);
				editors.put(zone, new Object[] { eff, picker });
			}
			box.getChildren().add(grid);
			var save = new Button("Save to mouse");
			save.getStyleClass().add("primary");
			save.setDisable(!onboard.isWritable());
			save.setOnAction(x -> {
				var todo = new LinkedHashMap<Integer, Lighting>();
				editors.forEach((zone, ed) -> {
					@SuppressWarnings("unchecked")
					String v = ((ComboBox<String>) ed[0]).getValue();
					Color c = ((ColorPicker) ed[1]).getValue();
					if (v.startsWith("Keep"))
						return;
					if (v.equals(EFFECTS.get(Lighting.STATIC)))
						todo.put(zone, new Lighting(Lighting.STATIC, 0, 0, List.of(toLed(c))));
					else if (v.equals(EFFECTS.get(Lighting.SPECTRUM)))
						todo.put(zone, new Lighting(Lighting.SPECTRUM, 0, 0x28, List.of()));
					else
						todo.put(zone, new Lighting(Lighting.OFF, 0, 0, List.of()));
				});
				int p = profile;
				if (p == activeProfile)
					stopLiveEffect();
				write("the lighting", () -> {
					for (var t : todo.entrySet())
						onboard.setLighting(p, t.getKey(), t.getValue());
				}, () -> lights.putAll(todo), null);
			});
			box.getChildren().add(save);
			var sep = new Region();
			sep.getStyleClass().add("divider");
			box.getChildren().add(sep);
		}

		box.getChildren().add(sectionTitle("Brightness"));
		var br = new Slider(0, 100, dev.getBrightness());
		br.setMaxWidth(420);
		var bv = new Label();
		bv.textProperty().bind(br.valueProperty().asString("%.0f%%"));
		br.valueChangingProperty().addListener((o, a, b) -> {
			if (!b)
				context.getSchedulerManager().get(SchedulerManager.Queue.DEVICE_IO)
						.execute(() -> dev.setBrightness((short) br.getValue()));
		});
		var brRow = new HBox(14, br, bv);
		brRow.setAlignment(Pos.CENTER_LEFT);
		box.getChildren().add(brRow);

		var sep2 = new Region();
		sep2.getStyleClass().add("divider");
		box.getChildren().addAll(sep2, sectionTitle(onboard != null ? "Live effects" : "Effect"));
		if (onboard != null)
			box.getChildren().add(hint(
					"Effects that run while Snake is open, like lights that react to sound. They show instead of the saved lighting until you pick \"Saved lighting\"."));
		var fx = context.getEffectManager();
		EffectAcquisition acq = fx.getRootAcquisition(dev);
		var handlers = new ArrayList<EffectHandler<?>>(fx.getEffects(dev));
		var live = new ComboBox<EffectHandler<?>>();
		live.getItems().add(null);
		live.getItems().addAll(handlers);
		live.setCellFactory(l -> new EffectCell());
		live.setButtonCell(new EffectCell());
		boolean saved = context.getPreferences(dev).getBoolean(EffectManager.PREF_SAVED_LIGHTING, false);
		live.setValue(acq == null || saved ? null : acq.getEffect(dev));
		liveEffect = live;
		var options = new Button("Options");
		options.getStyleClass().add("ghost");
		options.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
				() -> !hasSettings(live.getValue()), live.valueProperty()));
		options.setOnAction(e -> showEffectOptions(live.getValue()));
		live.setOnAction(e -> {
			EffectHandler<?> h = live.getValue();
			if (acq == null)
				return;
			if (h == null) {
				stopLiveEffect();
				if (onboard != null)
					reapplySavedLighting();
			} else {
				context.getPreferences(dev).putBoolean(EffectManager.PREF_SAVED_LIGHTING, false);
				acq.activate(dev, h);
			}
		});
		var fxRow = new HBox(10, live, options);
		fxRow.setAlignment(Pos.CENTER_LEFT);
		box.getChildren().add(fxRow);
		return box;
	}

	private ComboBox<EffectHandler<?>> liveEffect;

	/** Stop Snake's live effect and remember that the saved lighting is wanted. */
	private void stopLiveEffect() {
		Device dev = getDevice();
		context.getPreferences(dev).putBoolean(EffectManager.PREF_SAVED_LIGHTING, true);
		EffectAcquisition acq = context.getEffectManager().getRootAcquisition(dev);
		if (acq != null && acq.getEffect(dev) != null)
			acq.remove(dev);
		if (liveEffect != null && liveEffect.getValue() != null)
			liveEffect.setValue(null);
	}

	private class EffectCell extends ListCell<EffectHandler<?>> {
		@Override
		protected void updateItem(EffectHandler<?> item, boolean empty) {
			super.updateItem(item, empty);
			if (empty)
				setText(null);
			else
				setText(item == null ? (onboard != null ? "Saved lighting" : "None") : item.getDisplayName());
		}
	}

	private static final List<Class<? extends AudioEffectMode>> AUDIO_MODES = List.of(PeaksAudioEffectMode.class,
			ExpandAudioEffectMode.class, BlobbyAudioEffectMode.class, BurstyAudioEffectMode.class,
			LineyAudioEffectMode.class);

	public static boolean hasSettings(EffectHandler<?> h) {
		if (h instanceof AudioEffectHandler)
			return true;
		if (h instanceof AbstractBackendEffectHandler<?> b) {
			var c = b.getBackendEffectClass();
			return c == Static.class || c == Breath.class || c == Wave.class || c == Reactive.class
					|| c == Ripple.class || c == Starlight.class;
		}
		return false;
	}

	/** A colour row: swatches plus a picker. Calls {@code onPick} with RGB. */
	private Node colourRow(int[] rgb, java.util.function.Consumer<int[]> onPick) {
		var picker = new ColorPicker(rgb == null ? Color.web("#44d62c") : Color.rgb(rgb[0], rgb[1], rgb[2]));
		picker.getStyleClass().add("button");
		picker.valueProperty().addListener((o, a, c) -> onPick.accept(new int[] { (int) Math.round(c.getRed() * 255),
				(int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255) }));
		var sw = new javafx.scene.layout.FlowPane(6, 6);
		for (Color c : SWATCHES) {
			var b = new Button();
			b.getStyleClass().add("swatch");
			b.setStyle("-fx-background-color: " + toHex(c) + ";");
			b.setOnAction(x -> picker.setValue(c));
			sw.getChildren().add(b);
		}
		return new VBox(8, sw, picker);
	}

	/** Segmented choice; calls {@code onPick} with the chosen index. */
	private Node segments(String[] labels, int selected, java.util.function.IntConsumer onPick) {
		var row = new HBox(6);
		row.getStyleClass().add("segments");
		var tg = new ToggleGroup();
		for (int i = 0; i < labels.length; i++) {
			int idx = i;
			var t = new ToggleButton(labels[i]);
			t.getStyleClass().add("segment");
			t.setToggleGroup(tg);
			t.setSelected(i == selected);
			t.setOnAction(e -> {
				t.setSelected(true);
				onPick.accept(idx);
			});
			row.getChildren().add(t);
		}
		return row;
	}

	/** A slider that calls {@code onSet} when the user lets go. */
	private Node slider(double min, double max, double value, java.util.function.DoubleConsumer onSet) {
		var sl = new Slider(min, max, Math.max(min, Math.min(max, value)));
		sl.valueChangingProperty().addListener((o, a, b) -> {
			if (!b)
				onSet.accept(sl.getValue());
		});
		sl.setOnMouseReleased(e -> onSet.accept(sl.getValue()));
		return sl;
	}

	/** Settings for the live effect, in the right-hand panel. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void showEffectOptions(EffectHandler<?> handler) {
		if (handler == null)
			return;
		closeEditor();
		Device dev = getDevice();
		var title = new Label(handler.getDisplayName());
		title.getStyleClass().add("sectionTitle");
		var body = new VBox(10);
		if (handler instanceof AudioEffectHandler a) {
			var am = context.getAudioManager();
			var cfg = context.getConfiguration();
			int[][] cols = { a.getColor1(), a.getColor2(), a.getColor3() };
			Class<? extends AudioEffectMode>[] mode = new Class[] { a.getMode() };
			Runnable apply = () -> a.configure(dev, mode[0], cols[0], cols[1], cols[2]);
			var modes = combo(AUDIO_MODES, c -> c.getSimpleName().replace("AudioEffectMode", ""));
			modes.setValue(mode[0]);
			modes.setOnAction(e -> {
				mode[0] = modes.getValue();
				apply.run();
			});
			body.getChildren().addAll(fieldLabel("Style"), modes);
			if (am.getError() != null)
				body.getChildren().add(hint("Sound isn't available: " + am.getError().getLocalizedMessage()));
			else {
				var src = combo(am.getSources(), s -> s.getName());
				src.setValue(am.getSource(cfg.getAudioSource()));
				src.setOnAction(e -> {
					if (src.getValue() != null)
						cfg.setAudioSource(src.getValue().getName());
				});
				body.getChildren().addAll(fieldLabel("Listen to"), src);
			}
			body.getChildren().addAll(fieldLabel("Sensitivity"),
					slider(0, 20, cfg.getAudioGain(), v -> cfg.setAudioGain((float) v)));
			String[] names = { "Quiet colour", "Middle colour", "Loud colour" };
			for (int i = 0; i < 3; i++) {
				int idx = i;
				body.getChildren().addAll(fieldLabel(names[i]), colourRow(cols[i], c -> {
					cols[idx] = c;
					apply.run();
				}));
			}
		} else if (handler instanceof AbstractBackendEffectHandler h && hasSettings(handler)) {
			Effect e = h.loadEffect(dev);
			Runnable apply = () -> {
				try {
					h.storeEffect(dev, (Effect) e.clone());
				} catch (CloneNotSupportedException ex) {
					throw new IllegalStateException(ex);
				}
			};
			if (e instanceof Static st) {
				body.getChildren().addAll(fieldLabel("Colour"), colourRow(st.getColor(), c -> {
					st.setColor(c);
					apply.run();
				}));
			} else if (e instanceof Breath br) {
				var colours = new VBox(10);
				Runnable fill = () -> {
					colours.getChildren().clear();
					if (br.getMode() == Breath.Mode.SINGLE)
						colours.getChildren().addAll(fieldLabel("Colour"), colourRow(br.getColor(), c -> {
							br.setColor(c);
							apply.run();
						}));
					else if (br.getMode() == Breath.Mode.DUAL)
						colours.getChildren().addAll(fieldLabel("First colour"), colourRow(br.getColor1(), c -> {
							br.setColor1(c);
							apply.run();
						}), fieldLabel("Second colour"), colourRow(br.getColor2(), c -> {
							br.setColor2(c);
							apply.run();
						}));
				};
				Breath.Mode[] bm = { Breath.Mode.SINGLE, Breath.Mode.DUAL, Breath.Mode.RANDOM };
				int sel = br.getMode() == null ? 2 : java.util.Arrays.asList(bm).indexOf(br.getMode());
				body.getChildren().addAll(fieldLabel("Colours"),
						segments(new String[] { "One", "Two", "Random" }, sel, i -> {
							br.setMode(bm[i]);
							fill.run();
							apply.run();
						}), colours);
				fill.run();
			} else if (e instanceof Wave w) {
				body.getChildren().addAll(fieldLabel("Direction"), segments(new String[] { "Backward", "Forward" },
						w.getDirection() == Wave.Direction.FORWARD ? 1 : 0, i -> {
							w.setDirection(Wave.Direction.values()[i]);
							apply.run();
						}));
			} else if (e instanceof Reactive r) {
				body.getChildren().addAll(fieldLabel("Speed"), slider(1, 255, r.getSpeed(), v -> {
					r.setSpeed((int) Math.round(v));
					apply.run();
				}), fieldLabel("Colour"), colourRow(r.getColor(), c -> {
					r.setColor(c);
					apply.run();
				}));
			} else if (e instanceof Ripple rp) {
				var colour = colourRow(rp.getColor(), c -> {
					rp.setColor(c);
					apply.run();
				});
				colour.setVisible(rp.getMode() == Ripple.Mode.SINGLE);
				colour.managedProperty().bind(colour.visibleProperty());
				body.getChildren().addAll(fieldLabel("Colour"), segments(new String[] { "Random", "One colour" },
						rp.getMode() == Ripple.Mode.SINGLE ? 1 : 0, i -> {
							rp.setMode(Ripple.Mode.values()[i]);
							colour.setVisible(i == 1);
							apply.run();
						}), colour, fieldLabel("Speed"), slider(1, 255, rp.getRefreshRate(), v -> {
							rp.setRefreshRate(v);
							apply.run();
						}));
			} else if (e instanceof Starlight sl) {
				var colours = new VBox(10);
				Runnable fill = () -> {
					colours.getChildren().clear();
					if (sl.getMode() == Starlight.Mode.SINGLE)
						colours.getChildren().add(colourRow(sl.getColor(), c -> {
							sl.setColor(c);
							apply.run();
						}));
					else if (sl.getMode() == Starlight.Mode.DUAL)
						colours.getChildren().addAll(colourRow(sl.getColor1(), c -> {
							sl.setColor1(c);
							apply.run();
						}), colourRow(sl.getColor2(), c -> {
							sl.setColor2(c);
							apply.run();
						}));
				};
				Starlight.Mode[] sm = { Starlight.Mode.SINGLE, Starlight.Mode.DUAL, Starlight.Mode.RANDOM };
				int sel = sl.getMode() == null ? 2 : java.util.Arrays.asList(sm).indexOf(sl.getMode());
				body.getChildren().addAll(fieldLabel("Colours"),
						segments(new String[] { "One", "Two", "Random" }, sel, i -> {
							sl.setMode(sm[i]);
							fill.run();
							apply.run();
						}), colours, fieldLabel("Speed"), slider(1, 255, sl.getSpeed(), v -> {
							sl.setSpeed((int) Math.round(v));
							apply.run();
						}));
				fill.run();
			}
		} else {
			body.getChildren().add(hint("This effect has no settings."));
		}
		var close = cancelButton("Done");
		editor.getChildren().setAll(title, body, close);
		editor.setVisible(true);
	}

	/** After stopping a live effect, put the profile's own lighting back on the mouse. */
	private void reapplySavedLighting() {
		if (!onboard.isWritable() || profile != activeProfile)
			return;
		var ls = new LinkedHashMap<>(lights);
		int p = profile;
		io.execute(() -> {
			try {
				withMouse(() -> {
					for (var e : ls.entrySet())
						onboard.setLighting(p, e.getKey(), e.getValue());
					return null;
				});
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Could not restore saved lighting.", e);
			}
		});
	}

	/*
	 * LEDs are linear but colours on screen are gamma encoded, so mixed colours
	 * (orange, purple, pink) look washed out on the mouse unless converted.
	 */
	private static final double LED_GAMMA = 2.2;

	static int[] toLed(Color c) {
		return new int[] { led(c.getRed()), led(c.getGreen()), led(c.getBlue()) };
	}

	private static int led(double v) {
		return (int) Math.round(Math.pow(v, LED_GAMMA) * 255);
	}

	/** Inverse of {@link #toLed(Color)}, for showing a colour read from the mouse. */
	static int[] fromLed(int[] rgb) {
		int[] o = new int[3];
		for (int i = 0; i < 3; i++)
			o[i] = (int) Math.round(Math.pow(rgb[i] / 255.0, 1 / LED_GAMMA) * 255);
		return o;
	}

	private static String toHex(Color c) {
		return String.format("#%02x%02x%02x", (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
				(int) Math.round(c.getBlue() * 255));
	}

	/* ---- Macros page ---- */

	/* Naga side plate keys, in plate order */
	private static final List<EventCode> SIDE_KEYS = List.of(EventCode.KEY_1, EventCode.KEY_2, EventCode.KEY_3,
			EventCode.KEY_4, EventCode.KEY_5, EventCode.KEY_6, EventCode.KEY_7, EventCode.KEY_8, EventCode.KEY_9,
			EventCode.KEY_0, EventCode.KEY_MINUS, EventCode.KEY_EQUAL);
	private static final String[] MACRO_KINDS = { "Press a key", "Type text", "Run a command", "Snake action",
			"Script", "Do nothing" };

	private final List<Button> macroRows = new ArrayList<>();

	private Node macrosPage() {
		var box = pageBox("Macros",
				"Make a button press a different key, type some text or run a command. These run through Snake, so Snake has to be open for them to work.");
		macroRows.clear();
		var mm = context.getMacroManager();
		MacroDevice md = mm.getMacroDevice(getDevice());
		if (md == null) {
			box.getChildren().add(hint("This device doesn't support Snake macros."));
			return box;
		}
		MacroBank bank;
		try {
			bank = mm.getMacroSystem().getActiveBank(md);
		} catch (Exception e) {
			box.getChildren().add(hint("Couldn't load macros: " + e.getMessage()));
			return box;
		}

		box.getChildren().add(macroSetRow(mm.getMacroSystem(), md));

		var add = new Button("Add a macro");
		add.getStyleClass().add("primary");
		add.setOnAction(e -> {
			KeySequence seq = SIDE_KEYS.stream().filter(c -> md.getSupportedInputEvents().contains(c))
					.map(c -> new KeySequence(KeyState.UP, c)).filter(k -> !bank.contains(k)).findFirst().orElse(null);
			try {
				if (seq == null)
					seq = bank.getNextFreeActivationSequence();
			} catch (IllegalStateException ise) {
				setStatus("Every button already has a macro.", true);
				return;
			}
			showMacroEditor(bank, md, null, seq);
		});
		box.getChildren().add(add);

		var macros = new ArrayList<>(bank.getMacros());
		macros.sort((a, b) -> Integer.compare(triggerOrder(a.getActivatedBy()), triggerOrder(b.getActivatedBy())));
		if (macros.isEmpty()) {
			box.getChildren().add(hint("No macros yet."));
		} else {
			var list = new VBox(8);
			list.setMaxWidth(520);
			for (Macro m : macros) {
				var t = new Label(triggerName(m.getActivatedBy()));
				t.getStyleClass().add("slotName");
				var a = new Label(describeMacro(m));
				a.getStyleClass().add("slotAction");
				var b = new Button();
				b.setGraphic(new VBox(1, t, a));
				b.getStyleClass().add("slot");
				b.setMaxWidth(Double.MAX_VALUE);
				b.setUserData(m);
				b.setOnAction(e -> showMacroEditor(bank, md, m, m.getActivatedBy()));
				macroRows.add(b);
				list.getChildren().add(b);
			}
			box.getChildren().add(list);
		}

		if (onboard != null)
			box.getChildren().add(hint(
					"Macros saved on the mouse (made in Synapse) show on the Buttons page as \"Macro (from Synapse)\"."));
		return box;
	}

	/** Pick, add, rename or delete macro sets (macrolib profiles). The picked set is the one in use. */
	private Node macroSetRow(uk.co.bithatch.macrolib.MacroSystem sys, MacroDevice md) {
		var profiles = new ArrayList<uk.co.bithatch.macrolib.MacroProfile>();
		try {
			sys.profiles(md).forEachRemaining(profiles::add);
		} catch (Exception e) {
			return hint("Couldn't load macro sets: " + e.getMessage());
		}
		var active = sys.getActiveProfile(md);
		var row = new HBox(10);
		row.setAlignment(Pos.CENTER_LEFT);
		var label = fieldLabel("Macro set");
		var pick = combo(profiles, uk.co.bithatch.macrolib.MacroProfile::getName);
		pick.setMaxWidth(260);
		pick.setValue(active);
		pick.setOnAction(e -> {
			var p = pick.getValue();
			if (p == null || p.equals(sys.getActiveProfile(md)))
				return;
			try {
				p.makeActive();
			} catch (Exception ex) {
				setStatus("Couldn't switch macro set: " + ex.getMessage(), true);
			}
			showPage("macros");
		});
		var add = new Button("New set");
		add.getStyleClass().add("ghost");
		add.setOnAction(e -> {
			int n = profiles.size() + 1;
			while (sys.getProfileWithName(md, "Set " + n) != null)
				n++;
			try {
				var p = sys.createProfile(md, "Set " + n);
				p.commit();
				p.makeActive();
			} catch (Exception ex) {
				setStatus("Couldn't add a macro set: " + ex.getMessage(), true);
			}
			showPage("macros");
		});
		var rename = new Button("Rename");
		rename.getStyleClass().add("ghost");
		rename.setOnAction(e -> {
			var p = pick.getValue();
			if (p == null)
				return;
			var tf = new javafx.scene.control.TextField(p.getName());
			tf.setPrefWidth(260);
			tf.setOnAction(x -> {
				if (!tf.getText().isBlank()) {
					p.setName(tf.getText().trim());
					p.commit();
				}
				showPage("macros");
			});
			tf.focusedProperty().addListener((o, a, b) -> {
				if (!b)
					tf.getOnAction().handle(null);
			});
			row.getChildren().set(1, tf);
			Platform.runLater(() -> {
				tf.requestFocus();
				tf.selectAll();
			});
		});
		row.getChildren().addAll(label, pick, add, rename);
		if (profiles.size() > 1) {
			var del = new Button("Delete set");
			del.getStyleClass().add("ghost");
			del.setOnAction(e -> {
				var p = pick.getValue();
				if (p == null)
					return;
				if (!"sure".equals(del.getUserData())) {
					del.setUserData("sure");
					del.setText("Delete \"" + p.getName() + "\" and its macros?");
					return;
				}
				try {
					for (var o : profiles)
						if (!o.equals(p)) {
							o.makeActive();
							break;
						}
					p.remove();
				} catch (Exception ex) {
					setStatus("Couldn't delete the macro set: " + ex.getMessage(), true);
				}
				showPage("macros");
			});
			row.getChildren().add(del);
		}
		return row;
	}

	private int triggerOrder(KeySequence seq) {
		EventCode c = seq.isEmpty() ? null : seq.get(0);
		int i = c == null ? -1 : SIDE_KEYS.indexOf(c);
		int base = i >= 0 ? i : 100 + (c == null ? 0 : c.ordinal());
		return base * 2 + (seq.getState() == KeyState.HELD ? 1 : 0);
	}

	private String triggerName(KeySequence seq) {
		if (seq.isEmpty())
			return "(no button)";
		String n = keyName(seq.get(0));
		return seq.getState() == KeyState.HELD ? n + ", held down" : n;
	}

	/** "A Key", "Left Shift", "Left Button" ... for an input event code. */
	static String keyText(EventCode k) {
		String keyName = String.valueOf(k);
		String txt = keyName.length() > 4 ? keyName.substring(4) : keyName;
		if (txt.length() > 3)
			return uk.co.bithatch.snake.ui.util.Strings.toName(txt);
		return keyName.startsWith("BTN_") ? txt + " Button" : txt + " Key";
	}

	private String keyName(EventCode c) {
		int i = SIDE_KEYS.indexOf(c);
		String n = keyText(c);
		if (i >= 0 && getDevice().getName().contains("Naga"))
			return "Side button " + (i + 1) + " (" + n + ")";
		return n;
	}

	private String describeMacro(Macro m) {
		String what;
		if (m instanceof UInputMacro u)
			what = u.getCode() == null ? "Presses nothing yet" : "Presses " + keyText(u.getCode());
		else if (m instanceof SimpleMacro s)
			what = "Types \"" + (s.getMacro() == null ? "" : s.getMacro()) + "\"";
		else if (m instanceof CommandMacro c)
			what = "Runs " + (c.getCommand() == null ? "nothing yet" : new File(c.getCommand()).getName());
		else if (m instanceof ActionMacro a)
			what = "Snake: " + a.getAction();
		else if (m instanceof ScriptMacro)
			what = "Runs a script";
		else
			what = "Does nothing";
		if (m.getRepeatMode() == RepeatMode.WHILE_HELD)
			what += ", repeats while held";
		else if (m.getRepeatMode() == RepeatMode.TOGGLE)
			what += ", repeat toggles on/off";
		return what;
	}

	private static String macroKind(Macro m) {
		if (m instanceof SimpleMacro)
			return MACRO_KINDS[1];
		if (m instanceof CommandMacro)
			return MACRO_KINDS[2];
		if (m instanceof ActionMacro)
			return MACRO_KINDS[3];
		if (m instanceof ScriptMacro)
			return MACRO_KINDS[4];
		if (m instanceof NoopMacro)
			return MACRO_KINDS[5];
		return MACRO_KINDS[0];
	}

	private Label fieldLabel(String text) {
		var l = new Label(text);
		l.getStyleClass().add("fieldLabel");
		l.setWrapText(true);
		return l;
	}

	private static <T> javafx.util.Callback<javafx.scene.control.ListView<T>, ListCell<T>> cells(
			java.util.function.Function<T, String> text) {
		return lv -> new ListCell<T>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : text.apply(item));
			}
		};
	}

	private <T> ComboBox<T> combo(java.util.Collection<T> items, java.util.function.Function<T, String> text) {
		var cb = new ComboBox<T>();
		cb.getItems().addAll(items);
		cb.setCellFactory(cells(text));
		cb.setButtonCell(cells(text).call(null));
		cb.setMaxWidth(Double.MAX_VALUE);
		cb.setVisibleRowCount(14);
		return cb;
	}

	/**
	 * Right-hand editor for one macro. {@code existing} is null for a new one.
	 * Nothing changes until Save.
	 */
	private void showMacroEditor(MacroBank bank, MacroDevice md, Macro existing, KeySequence seq) {
		closeEditor();
		macroRows.forEach(b -> {
			b.getStyleClass().remove("selected");
			if (existing != null && b.getUserData() == existing)
				b.getStyleClass().add("selected");
		});
		var title = new Label(existing == null ? "New macro" : triggerName(existing.getActivatedBy()));
		title.getStyleClass().add("sectionTitle");
		title.setWrapText(true);

		/* Trigger */
		var triggers = new ArrayList<EventCode>(SIDE_KEYS.stream()
				.filter(c -> md.getSupportedInputEvents().contains(c)).toList());
		for (EventCode c : EventCode.filteredForType(md.getSupportedInputEvents(), EventCode.Type.EV_KEY))
			if (!triggers.contains(c))
				triggers.add(c);
		var trigger = combo(triggers, this::keyName);
		trigger.setValue(seq.isEmpty() ? null : seq.get(0));
		var held = new CheckBox("Only when held down");
		held.setSelected(seq.getState() == KeyState.HELD);

		/* What it does */
		var grid = new GridPane();
		grid.getStyleClass().add("choiceGrid");
		var tg = new ToggleGroup();
		var detail = new VBox(8);
		var actions = new ArrayList<>(context.getMacroManager().getMacroSystem().getActions().values());

		var keyBox = combo(EventCode.filteredForType(java.util.Arrays.asList(EventCode.values()), EventCode.Type.EV_KEY),
				DeviceShell::keyText);
		var text = new javafx.scene.control.TextField();
		var command = new javafx.scene.control.TextField();
		var args = new javafx.scene.control.TextArea();
		args.setPrefRowCount(3);
		var action = combo(actions, uk.co.bithatch.macrolib.Action::getId);
		var script = new javafx.scene.control.TextArea();
		script.setPrefRowCount(6);
		if (existing instanceof UInputMacro u)
			keyBox.setValue(u.getCode());
		else if (existing instanceof SimpleMacro s)
			text.setText(s.getMacro());
		else if (existing instanceof CommandMacro c) {
			command.setText(c.getCommand());
			args.setText(c.getArguments() == null ? "" : String.join("\n", c.getArguments()));
		} else if (existing instanceof ActionMacro a)
			action.setValue(context.getMacroManager().getMacroSystem().getActions().get(a.getAction()));
		else if (existing instanceof ScriptMacro sc && sc.getScript() != null)
			script.setText(String.join("\n", sc.getScript()));
		if (action.getValue() == null && !actions.isEmpty())
			action.setValue(actions.get(0));

		String[] kind = { existing == null ? MACRO_KINDS[1] : macroKind(existing) };
		Runnable showDetail = () -> {
			detail.getChildren().clear();
			switch (kind[0]) {
			case "Press a key" -> detail.getChildren().addAll(fieldLabel("Key to press"), keyBox);
			case "Type text" -> detail.getChildren().addAll(fieldLabel("Text to type"), text,
					hint("\\r = Enter, \\t = Tab, \\b = Backspace, \\e = Escape, \\p = short pause."));
			case "Run a command" -> {
				var browse = new Button("Browse");
				browse.getStyleClass().add("ghost");
				browse.setOnAction(e -> {
					var fc = new FileChooser();
					var f = fc.showOpenDialog(getScene().getWindow());
					if (f != null)
						command.setText(f.getAbsolutePath());
				});
				HBox.setHgrow(command, Priority.ALWAYS);
				detail.getChildren().addAll(fieldLabel("Program"), new HBox(8, command, browse),
						fieldLabel("Arguments, one per line"), args);
			}
			case "Snake action" -> detail.getChildren().addAll(fieldLabel("Action"), action);
			case "Script" -> detail.getChildren().addAll(fieldLabel("Script"), script,
					hint("Macro script commands, one per line."));
			default -> detail.getChildren().add(hint("The button won't do anything."));
			}
		};
		for (int i = 0; i < MACRO_KINDS.length; i++) {
			String k = MACRO_KINDS[i];
			if (k.equals("Snake action") && actions.isEmpty())
				continue;
			var tb = new ToggleButton(k);
			tb.getStyleClass().add("segment");
			tb.setMaxWidth(Double.MAX_VALUE);
			tb.setToggleGroup(tg);
			tb.setSelected(k.equals(kind[0]));
			tb.setOnAction(e -> {
				tb.setSelected(true);
				kind[0] = k;
				showDetail.run();
			});
			int n = grid.getChildren().size();
			grid.add(tb, n % 2, n / 2);
		}
		showDetail.run();

		/* Repeat */
		var repeat = combo(List.of(RepeatMode.values()), r -> switch (r) {
		case NONE -> "Once per press";
		case WHILE_HELD -> "Repeat while held";
		case TOGGLE -> "Press to start repeating, again to stop";
		});
		repeat.setValue(existing == null ? RepeatMode.NONE : existing.getRepeatMode());

		var save = new Button("Save");
		save.getStyleClass().add("primary");
		save.setDefaultButton(true);
		save.setOnAction(e -> {
			EventCode code = trigger.getValue();
			if (code == null) {
				setStatus("Pick a button first.", true);
				return;
			}
			var newSeq = new KeySequence(held.isSelected() ? KeyState.HELD : KeyState.UP, code);
			Macro other = bank.getMacro(newSeq);
			if (other != null && other != existing) {
				setStatus(triggerName(newSeq) + " already has a macro.", true);
				return;
			}
			Macro m = switch (kind[0]) {
			case "Press a key" -> new UInputMacro(newSeq, keyBox.getValue());
			case "Type text" -> new SimpleMacro(newSeq, text.getText());
			case "Run a command" -> {
				var c = new CommandMacro(newSeq, command.getText().trim());
				c.setArguments(args.getText().isBlank() ? List.of()
						: java.util.Arrays.stream(args.getText().split("\n")).filter(a -> !a.isBlank()).toList());
				yield c;
			}
			case "Snake action" -> new ActionMacro(newSeq, action.getValue() == null ? null : action.getValue().getId());
			case "Script" -> new ScriptMacro(newSeq, script.getText().split("\n"));
			default -> new NoopMacro(newSeq);
			};
			m.setRepeatMode(repeat.getValue());
			if (existing != null) {
				m.setName(existing.getName());
				m.setRepeatDelay(existing.getRepeatDelay());
			}
			try {
				context.getMacroManager().validate(m);
			} catch (uk.co.bithatch.snake.lib.ValidationException ve) {
				setStatus(macroError(ve.getMessage()), true);
				return;
			}
			try {
				if (existing != null)
					bank.remove(existing);
				bank.add(m);
				bank.commit();
			} catch (Exception ex) {
				setStatus("Couldn't save the macro: " + ex.getMessage(), true);
				return;
			}
			setStatus("Macro saved.", false);
			closeEditor();
			showPage("macros");
		});
		var acts = new HBox(10, save);
		if (existing != null) {
			var del = new Button("Delete");
			del.getStyleClass().add("ghost");
			del.setOnAction(e -> {
				try {
					bank.remove(existing);
					bank.commit();
				} catch (Exception ex) {
					setStatus("Couldn't delete the macro: " + ex.getMessage(), true);
					return;
				}
				setStatus("Macro deleted.", false);
				closeEditor();
				showPage("macros");
			});
			acts.getChildren().add(del);
		}
		acts.getChildren().add(cancelButton("Cancel"));

		editor.getChildren().setAll(title, fieldLabel("Button"), trigger, held, fieldLabel("Make it"), grid, detail,
				fieldLabel("Repeat"), repeat, acts);
		editor.setVisible(true);
	}

	private static String macroError(String key) {
		return switch (key) {
		case "error.missingUInputCode" -> "Pick a key to press.";
		case "error.missingSimpleMacro" -> "Type the text first.";
		case "error.missingScript" -> "The script is empty.";
		case "error.missingCommand" -> "Pick a program to run.";
		default -> key.startsWith("error.") ? "Pick an action." : key;
		};
	}

	/* ---- Devices page ---- */

	private Node devicesPage() {
		var box = pageBox("Devices", "Pick a device to set it up.");
		var flow = new javafx.scene.layout.FlowPane(22, 22);
		try {
			for (Device d : context.getBackend().getDevices()) {
				var pic = new ImageView(new Image(
						context.getDefaultImage(d.getType(), context.getCache().getCachedImage(d.getImage())), true));
				pic.setPreserveRatio(true);
				pic.setFitWidth(190);
				pic.setFitHeight(160);
				var picBox = new StackPane(pic);
				picBox.setMinHeight(166);
				var n = new Label(d.getName());
				n.getStyleClass().add("sectionTitle");
				n.setMaxWidth(204);
				String type = d.getType().name().charAt(0) + d.getType().name().substring(1).toLowerCase().replace('_', ' ');
				var sub = new Label(d == getDevice() ? type + ", open now" : type);
				sub.getStyleClass().add("hint");
				var card = new Button();
				card.setGraphic(new VBox(6, picBox, n, sub));
				card.getStyleClass().add("deviceCard");
				card.setMaxHeight(Region.USE_PREF_SIZE);
				card.setMinHeight(Region.USE_PREF_SIZE);
				if (d == getDevice())
					card.getStyleClass().add("selected");
				card.setOnAction(e -> {
					if (d == getDevice())
						showPage("buttons");
					else
						try {
							context.openDevice(d);
						} catch (Exception ex) {
							error(ex);
						}
				});
				flow.getChildren().add(card);
			}
		} catch (Exception e) {
			box.getChildren().add(hint("Couldn't list devices: " + e.getMessage()));
		}
		box.getChildren().add(flow);
		return box;
	}

	/* ---- Settings page ---- */

	private Node settingsPage() {
		var box = pageBox("Settings", null);
		var grid = new GridPane();
		grid.getStyleClass().add("settingsGrid");
		int row = 0;

		var cfg = context.getConfiguration();
		var ps = PlatformService.isPlatformSupported() ? PlatformService.get() : null;

		var tray = combo(List.of(TrayIcon.values()), t -> switch (t) {
		case OFF -> "No tray icon";
		case AUTO -> "Automatic";
		case DARK -> "Dark";
		case LIGHT -> "Light";
		default -> "Colour";
		});
		tray.setValue(cfg.getTrayIcon());
		tray.setMaxWidth(200);
		grid.addRow(row++, settingText("Tray icon", "The icon in the panel. Turning it off also stops Snake starting with the computer."), tray);

		CheckBox login = null;
		if (ps != null) {
			var lg = new CheckBox("On");
			lg.setSelected(ps.isStartOnLogin());
			lg.setDisable(cfg.getTrayIcon() == TrayIcon.OFF);
			lg.setOnAction(e -> {
				try {
					ps.setStartOnLogin(lg.isSelected() && cfg.getTrayIcon() != TrayIcon.OFF);
				} catch (Exception ex) {
					setStatus("Couldn't change start with the computer: " + ex.getMessage(), true);
				}
			});
			grid.addRow(row++, settingText("Start with the computer", "Open Snake in the tray when you log in, so macros and effects keep working."), lg);
			login = lg;
		}
		var fLogin = login;
		tray.setOnAction(e -> {
			cfg.setTrayIcon(tray.getValue());
			if (fLogin != null) {
				fLogin.setDisable(tray.getValue() == TrayIcon.OFF);
				try {
					ps.setStartOnLogin(fLogin.isSelected() && tray.getValue() != TrayIcon.OFF);
				} catch (Exception ex) {
					setStatus("Couldn't change start with the computer: " + ex.getMessage(), true);
				}
			}
		});

		if (context.getBackend().getCapabilities().contains(uk.co.bithatch.snake.lib.Capability.BATTERY)) {
			var bat = new CheckBox("Show");
			bat.setSelected(cfg.isShowBattery());
			var low = new CheckBox("Only when low");
			low.setSelected(cfg.isWhenLow());
			low.setDisable(!bat.isSelected());
			bat.setOnAction(e -> {
				cfg.setShowBattery(bat.isSelected());
				low.setDisable(!bat.isSelected());
			});
			low.setOnAction(e -> cfg.setWhenLow(low.isSelected()));
			grid.addRow(row++, settingText("Battery in tray", "Show the battery level on the tray icon."), new HBox(14, bat, low));
		}

		if (ps != null && ps.isUpdateableApp()) {
			var check = new CheckBox("Check");
			check.setSelected(ps.isCheckForUpdates());
			var auto = new CheckBox("Install automatically");
			auto.setSelected(ps.isUpdateAutomatically());
			auto.setDisable(!check.isSelected());
			check.setOnAction(e -> {
				ps.setCheckForUpdates(check.isSelected());
				auto.setDisable(!check.isSelected());
			});
			auto.setOnAction(e -> ps.setUpdateAutomatically(auto.isSelected()));
			var upd = new HBox(14, check, auto);
			upd.setAlignment(Pos.CENTER_LEFT);
			String desc = "Snake updates itself from its GitHub page.";
			if (ps.isUpdateAvailable() && ps.isCheckForUpdates()) {
				desc = "Update available: " + ps.getAvailableVersion() + " (you have " + ps.getInstalledVersion() + ").";
				var now = new Button("Update now");
				now.getStyleClass().add("primary");
				now.setOnAction(e -> {
					now.setDisable(true);
					ps.startUpdate();
				});
				upd.getChildren().add(now);
			}
			grid.addRow(row++, settingText("Updates", desc), upd);
		}

		if (onboard != null) {
			var backup = new Button("Back up");
			backup.getStyleClass().add("ghost");
			backup.setOnAction(e -> backupTo());
			var restore = new Button("Restore");
			restore.getStyleClass().add("ghost");
			restore.setOnAction(e -> restoreFrom());
			grid.addRow(row++,
					settingText("Mouse profiles", "Save every profile on the mouse to a file, or put a saved file back."),
					new HBox(8, backup, restore));
			if (!onboard.isVerified()) {
				var exp = new CheckBox("Allow");
				exp.setSelected(onboard.isExperimentalWrites());
				exp.setOnAction(e -> {
					onboard.setExperimentalWrites(exp.isSelected());
					context.getPreferences(getDevice()).putBoolean("onboardExperimental", exp.isSelected());
				});
				grid.addRow(row++, settingText("Experimental saving",
						"Saving to this mouse hasn't been tested yet. Snake backs up all profiles before the first save."),
						exp);
			}
		}

		box.getChildren().add(grid);

		var sep = new Region();
		sep.getStyleClass().add("divider");
		box.getChildren().addAll(sep, sectionTitle("About Snake"));
		String version = ps == null ? "unknown" : ps.getInstalledVersion();
		String backend;
		try {
			backend = context.getBackend().getName() + " " + context.getBackend().getVersion();
		} catch (Exception e) {
			backend = context.getBackend().getName();
		}
		box.getChildren().addAll(hint("Version " + version + ", using " + backend + "."),
				hint("Snake was made by Brett Smith (2020) and is free software under the GPL 3."));
		var changes = new Button("What's new");
		changes.getStyleClass().add("ghost");
		changes.setOnAction(e -> showChanges());
		box.getChildren().add(changes);
		return box;
	}

	/** The change log (CHANGES.md), as plain text in the right-hand panel. */
	public void showChanges() {
		closeEditor();
		var title = new Label("What's new");
		title.getStyleClass().add("sectionTitle");
		var body = new VBox(4);
		File dir = new File(System.getProperty("user.dir"));
		File f = new File(dir, "pom.xml").exists() ? new File(dir.getParentFile(), "CHANGES.md")
				: new File(dir, "docs" + File.separator + "CHANGES.md");
		try {
			for (String line : Files.readAllLines(f.toPath())) {
				if (line.isBlank())
					continue;
				Label l;
				if (line.startsWith("#")) {
					l = new Label(line.replaceFirst("^#+\\s*", ""));
					l.getStyleClass().add("fieldLabel");
				} else {
					l = hint(line.replaceFirst("^\\s*[-*]\\s*", "\u2022 ").replace("`", "").replace("**", ""));
				}
				l.setWrapText(true);
				body.getChildren().add(l);
			}
		} catch (Exception e) {
			body.getChildren().add(hint("The change log couldn't be found."));
		}
		var sp = new ScrollPane(body);
		sp.setFitToWidth(true);
		sp.getStyleClass().add("shellScroll");
		VBox.setVgrow(sp, Priority.ALWAYS);
		editor.getChildren().setAll(title, sp, cancelButton("Close"));
		editor.setVisible(true);
	}

	private Node settingText(String title, String desc) {
		var t = new Label(title);
		t.getStyleClass().add("settingName");
		var d = new Label(desc);
		d.getStyleClass().add("hint");
		d.setWrapText(true);
		d.setMaxWidth(440);
		return new VBox(2, t, d);
	}

	/* ---- saving and backups ---- */

	interface IOTask {
		void run() throws Exception;
	}

	private void write(String what, IOTask task, Runnable onDone, Runnable onFail) {
		setStatus("Saving " + what + "...", false);
		io.execute(() -> {
			try {
				withMouse(() -> {
					if (!backedUp) {
						Path f = backupDir().resolve("before-first-save-" + stamp() + ".json");
						onboard.backup(f);
						backedUp = true;
						LOG.log(Level.INFO, "Backed up onboard profiles to " + f);
					}
					task.run();
					return null;
				});
				Platform.runLater(() -> {
					setStatus("Saved " + what + " to " + profileName(profile) + ".", false);
					if (onDone != null)
						onDone.run();
				});
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Save failed.", e);
				Platform.runLater(() -> {
					setStatus("Couldn't save " + what + ": " + e.getMessage(), true);
					if (onFail != null)
						onFail.run();
				});
			}
		});
	}

	private Path backupDir() {
		return Path.of(System.getProperty("user.home"), ".local", "share", "snake", "onboard-backups");
	}

	private static String stamp() {
		return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
	}

	private void backupTo() {
		var fc = new FileChooser();
		fc.setTitle("Back up mouse profiles");
		try {
			Files.createDirectories(backupDir());
			fc.setInitialDirectory(backupDir().toFile());
		} catch (Exception e) {
		}
		fc.setInitialFileName("profiles-" + stamp() + ".json");
		File f = fc.showSaveDialog(getScene().getWindow());
		if (f == null)
			return;
		setStatus("Backing up...", false);
		io.execute(() -> {
			try {
				withMouse(() -> {
					onboard.backup(f.toPath());
					return null;
				});
				Platform.runLater(() -> setStatus("Backed up to " + f.getName() + ".", false));
			} catch (Exception e) {
				Platform.runLater(() -> setStatus("Backup failed: " + e.getMessage(), true));
			}
		});
	}

	private void restoreFrom() {
		if (!onboard.isWritable()) {
			setStatus("Saving to this mouse is off.", true);
			return;
		}
		var fc = new FileChooser();
		fc.setTitle("Restore mouse profiles");
		if (Files.isDirectory(backupDir()))
			fc.setInitialDirectory(backupDir().toFile());
		File f = fc.showOpenDialog(getScene().getWindow());
		if (f == null)
			return;
		write("all profiles from " + f.getName(), () -> onboard.restore(f.toPath()), this::reloadProfile, null);
	}
}
