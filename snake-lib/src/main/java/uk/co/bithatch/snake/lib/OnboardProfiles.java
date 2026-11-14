package uk.co.bithatch.snake.lib;

import java.io.IOException;
import java.util.List;

/**
 * Profiles stored in the device's own memory (e.g. made with Synapse). Slot 0 is
 * the live (unsaved) state, slots 1..{@link #getProfileCount()} are the stored
 * profiles.
 */
public interface OnboardProfiles {

	/** What a button does. {@code params} are the raw device bytes for the type. */
	record Action(int type, byte[] params) {
		public static final int DISABLED = 0x00;
		public static final int MOUSE = 0x01;
		public static final int KEY = 0x02;
		public static final int MACRO = 0x04;
		public static final int MACRO_REPEAT = 0x05;
		public static final int DPI = 0x06;
		public static final int PROFILE = 0x07;
		public static final int SCROLL = 0x0e;

		public static Action key(int modifiers, int hidUsage) {
			return new Action(KEY, new byte[] { (byte) modifiers, (byte) hidUsage });
		}

		public static Action mouse(int button) {
			return new Action(MOUSE, new byte[] { (byte) button });
		}

		public static Action disabled() {
			return new Action(DISABLED, new byte[0]);
		}

		/** Keyboard modifier bitmask (Ctrl=1, Shift=2, Alt=4, Super=8). Synapse sometimes stores only the usage. */
		public int keyModifiers() {
			return type == KEY && params.length > 1 ? params[0] & 0xff : 0;
		}

		public int keyUsage() {
			return type == KEY && params.length > 0 ? params[params.length - 1] & 0xff : 0;
		}
	}

	/**
	 * Lighting stored for one LED zone of a profile. Device bytes:
	 * effect, mode, speed, colour count, colours (RGB).
	 */
	record Lighting(int effect, int mode, int speed, List<int[]> colors) {
		public static final int STATIC = 0x01;
		public static final int BREATH = 0x02;
		public static final int SPECTRUM = 0x03;
		public static final int WAVE = 0x04;
		public static final int REACTIVE = 0x05;
		public static final int OFF = 0x00;
	}

	/** Number of profile slots in the device's memory. */
	int getProfileCount();

	/**
	 * Slots the profile button cycles through, in order (e.g. 1, 3, 4). Other
	 * slots hold old data the mouse doesn't use.
	 */
	default List<Integer> getProfiles() throws IOException {
		var l = new java.util.ArrayList<Integer>();
		for (int i = 1; i <= getProfileCount(); i++)
			l.add(i);
		return l;
	}

	/**
	 * True when saving has been tested on this model. Untested models only save
	 * when {@link #setExperimentalWrites(boolean)} is on.
	 */
	boolean isVerified();

	boolean isExperimentalWrites();

	void setExperimentalWrites(boolean experimentalWrites);

	default boolean isWritable() {
		return isVerified() || isExperimentalWrites();
	}

	/** Model name for known devices, or null. */
	String getModelName();

	/** Active profile slot as selected with the mouse's profile button. */
	int getActiveProfile() throws IOException;

	/** Button ids this device has (all plates). */
	List<Integer> getButtonIds();

	/** Human name of a button, e.g. "Side 1". */
	String getButtonName(int buttonId);

	/** Group a button belongs to, e.g. "Top", "2-button plate". */
	String getButtonGroup(int buttonId);

	/** Write every profile's settings to a JSON file. */
	default void backup(java.nio.file.Path file) throws IOException {
		var root = new com.google.gson.JsonObject();
		root.addProperty("model", getModelName());
		var profiles = new com.google.gson.JsonArray();
		for (int p = 1; p <= getProfileCount(); p++) {
			var po = new com.google.gson.JsonObject();
			po.addProperty("profile", p);
			var st = getDpiStages(p);
			po.addProperty("dpiActive", st.active());
			var sa = new com.google.gson.JsonArray();
			for (int d : st.dpi())
				sa.add(d);
			po.add("dpiStages", sa);
			var bo = new com.google.gson.JsonObject();
			for (int b : getButtonIds()) {
				Action a = getButton(p, b);
				var ao = new com.google.gson.JsonArray();
				ao.add(a.type());
				for (byte x : a.params())
					ao.add(x & 0xff);
				bo.add(String.valueOf(b), ao);
			}
			po.add("buttons", bo);
			var lo = new com.google.gson.JsonObject();
			for (int z : getLedZones()) {
				Lighting l = getLighting(p, z);
				var la = new com.google.gson.JsonArray();
				la.add(l.effect());
				la.add(l.mode());
				la.add(l.speed());
				for (int[] c : l.colors())
					for (int v : c)
						la.add(v);
				lo.add(String.valueOf(z), la);
			}
			po.add("lighting", lo);
			profiles.add(po);
		}
		root.add("profiles", profiles);
		java.nio.file.Files.createDirectories(file.getParent());
		java.nio.file.Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
	}

	/** Restore a file written by {@link #backup(java.nio.file.Path)}. */
	default void restore(java.nio.file.Path file) throws IOException {
		var root = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
		for (var pe : root.getAsJsonArray("profiles")) {
			var po = pe.getAsJsonObject();
			int p = po.get("profile").getAsInt();
			var sa = po.getAsJsonArray("dpiStages");
			int[] d = new int[sa.size()];
			for (int i = 0; i < d.length; i++)
				d[i] = sa.get(i).getAsInt();
			setDpiStages(p, new DpiStages(po.get("dpiActive").getAsInt(), d));
			for (var e : po.getAsJsonObject("buttons").entrySet()) {
				var a = e.getValue().getAsJsonArray();
				byte[] params = new byte[a.size() - 1];
				for (int i = 1; i < a.size(); i++)
					params[i - 1] = (byte) a.get(i).getAsInt();
				setButton(p, Integer.parseInt(e.getKey()), new Action(a.get(0).getAsInt(), params));
			}
			for (var e : po.getAsJsonObject("lighting").entrySet()) {
				var a = e.getValue().getAsJsonArray();
				var cols = new java.util.ArrayList<int[]>();
				for (int i = 3; i + 2 < a.size(); i += 3)
					cols.add(new int[] { a.get(i).getAsInt(), a.get(i + 1).getAsInt(), a.get(i + 2).getAsInt() });
				setLighting(p, Integer.parseInt(e.getKey()),
						new Lighting(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), cols));
			}
		}
	}

	Action getButton(int profile, int buttonId) throws IOException;

	void setButton(int profile, int buttonId, Action action) throws IOException;

	int getDpi(int profile) throws IOException;

	void setDpi(int profile, int dpi) throws IOException;

	/** DPI stages (x values) and the index (0 based) of the active one. */
	record DpiStages(int active, int[] dpi) {
	}

	DpiStages getDpiStages(int profile) throws IOException;

	void setDpiStages(int profile, DpiStages stages) throws IOException;

	/** LED zone ids (e.g. scroll wheel, logo). */
	List<Integer> getLedZones();

	String getLedZoneName(int zone);

	Lighting getLighting(int profile, int zone) throws IOException;

	void setLighting(int profile, int zone, Lighting lighting) throws IOException;
}
