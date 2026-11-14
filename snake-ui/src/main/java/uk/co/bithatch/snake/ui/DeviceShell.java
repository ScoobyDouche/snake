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
import uk.co.bithatch.snake.lib.Device;
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
	}

	@Override
	protected void onDeviceCleanUp() {
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
		var handlers = new ArrayList<EffectHandler<?, ?>>(fx.getEffects(dev));
		var live = new ComboBox<EffectHandler<?, ?>>();
		live.getItems().add(null);
		live.getItems().addAll(handlers);
		live.setCellFactory(l -> new EffectCell());
		live.setButtonCell(new EffectCell());
		boolean saved = context.getPreferences(dev).getBoolean(EffectManager.PREF_SAVED_LIGHTING, false);
		live.setValue(acq == null || saved ? null : acq.getEffect(dev));
		liveEffect = live;
		var options = new Button("Options");
		options.getStyleClass().add("ghost");
		options.disableProperty().bind(live.valueProperty().isNull());
		options.setOnAction(e -> openEffectOptions(live.getValue()));
		live.setOnAction(e -> {
			EffectHandler<?, ?> h = live.getValue();
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

	private ComboBox<EffectHandler<?, ?>> liveEffect;

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

	private class EffectCell extends ListCell<EffectHandler<?, ?>> {
		@Override
		protected void updateItem(EffectHandler<?, ?> item, boolean empty) {
			super.updateItem(item, empty);
			if (empty)
				setText(null);
			else
				setText(item == null ? (onboard != null ? "Saved lighting" : "None") : item.getDisplayName());
		}
	}

	@SuppressWarnings("unchecked")
	private void openEffectOptions(EffectHandler<?, ?> handler) {
		if (handler == null || !handler.hasOptions())
			return;
		var c = (AbstractEffectController<?, EffectHandler<?, ?>>) context.push(handler.getOptionsController(), this,
				Direction.FROM_RIGHT);
		try {
			c.setRegion(getDevice());
			c.setEffectHandler(handler);
		} catch (Exception e) {
			error(e);
		}
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

	private Node macrosPage() {
		var box = pageBox("Macros",
				"Record a set of key presses and put it on a button. Macros made here run through Snake, so Snake has to be open for them to work.");
		boolean ok = context.getMacroManager().isSupported(getDevice())
				|| getDevice().getCapabilities().contains(uk.co.bithatch.snake.lib.Capability.MACROS);
		if (ok) {
			var open = new Button("Open the macro editor");
			open.getStyleClass().add("primary");
			open.setOnAction(e -> {
				try {
					context.editMacros(this);
				} catch (Exception ex) {
					error(ex);
				}
			});
			box.getChildren().add(open);
		} else
			box.getChildren().add(hint("This device doesn't support macros."));
		if (onboard != null)
			box.getChildren().add(hint(
					"Macros saved on the mouse (made in Synapse) show on the Buttons page as \"Macro (from Synapse)\". You can keep them or replace them there."));
		return box;
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

		var more = new Button("Open");
		more.getStyleClass().add("ghost");
		more.setOnAction(e -> context.push(Options.class, this, Direction.FROM_RIGHT));
		grid.addRow(row++, settingText("Snake options", "Theme, tray icon, start with the computer, updates and more."),
				more);

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

		var about = new Button("Open");
		about.getStyleClass().add("ghost");
		about.setOnAction(e -> context.push(About.class, this, Direction.FROM_RIGHT));
		grid.addRow(row++, settingText("About Snake", "Version and credits."), about);
		box.getChildren().add(grid);
		return box;
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
