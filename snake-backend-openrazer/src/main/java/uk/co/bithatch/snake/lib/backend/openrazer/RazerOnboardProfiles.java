package uk.co.bithatch.snake.lib.backend.openrazer;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.bithatch.snake.lib.OnboardProfiles;

/**
 * Onboard profiles of Razer mice over raw HID. Worked out on the Naga Trinity
 * (1532:0067); other mice are detected with read-only probes and can only save
 * with experimental writes switched on.
 * <ul>
 * <li>02/8c get button [profile, button, 0] / 02/0c set (byte 2 must be 0)</li>
 * <li>04/85 get DPI / 04/05 set; 04/86 get DPI stages / 04/06 set</li>
 * <li>0f/82 get lighting [profile, led] (reply byte 0 is always 0) / 0f/02 set</li>
 * <li>05/84 get active profile</li>
 * </ul>
 */
public class RazerOnboardProfiles implements OnboardProfiles, Closeable {

	public static final int VID = 0x1532;

	/** What we know about a model. */
	public record Model(String name, int transactionId, int profiles, Map<Integer, String[]> buttons,
			Map<Integer, String> leds, boolean verified) {
	}

	private static final Map<Integer, String> LED_NAMES = Map.of(0x01, "Scroll wheel", 0x04, "Logo", 0x05,
			"Backlight", 0x0a, "Left side", 0x0b, "Right side");

	private static final Map<Integer, Model> KNOWN = new LinkedHashMap<>();
	static {
		var b = new LinkedHashMap<Integer, String[]>();
		b.put(0x01, new String[] { "Top", "Left click" });
		b.put(0x02, new String[] { "Top", "Right click" });
		b.put(0x03, new String[] { "Top", "Wheel click" });
		b.put(0x34, new String[] { "Top", "Wheel up" });
		b.put(0x35, new String[] { "Top", "Wheel down" });
		b.put(0x09, new String[] { "Top", "Wheel left" });
		b.put(0x0a, new String[] { "Top", "Wheel right" });
		b.put(0x0b, new String[] { "Top", "DPI up" });
		b.put(0x0c, new String[] { "Top", "DPI down" });
		b.put(0x0e, new String[] { "Bottom", "Profile button" });
		b.put(0x04, new String[] { "2-button plate", "Top" });
		b.put(0x05, new String[] { "2-button plate", "Bottom" });
		for (int i = 0; i < 7; i++)
			b.put(0x50 + i, new String[] { "7-button plate", String.valueOf(i + 1) });
		for (int i = 0; i < 12; i++)
			b.put(0x40 + i, new String[] { "12-button plate", String.valueOf(i + 1) });
		KNOWN.put(0x0067, new Model("Razer Naga Trinity", 0x1f, 5, b, leds(0x01, 0x04), true));
	}

	private static Map<Integer, String> leds(int... ids) {
		var m = new LinkedHashMap<Integer, String>();
		for (int id : ids)
			m.put(id, LED_NAMES.getOrDefault(id, "LED " + id));
		return m;
	}

	private final RazerHidraw hid;
	private final Model model;
	private boolean experimentalWrites;

	public RazerOnboardProfiles(RazerHidraw hid, Model model) {
		this.hid = hid;
		this.model = model;
		hid.setTransactionId(model.transactionId());
	}

	/**
	 * Open a Razer mouse's onboard profiles. Returns null if the device is not
	 * found or does not answer onboard profile commands.
	 */
	public static RazerOnboardProfiles open(int pid) throws IOException {
		var h = RazerHidraw.open(VID, pid);
		if (h == null)
			return null;
		try {
			Model m = KNOWN.get(pid);
			if (m == null)
				m = detect(h);
			if (m == null) {
				h.close();
				return null;
			}
			return new RazerOnboardProfiles(h, m);
		} catch (IOException | RuntimeException e) {
			h.close();
			throw e;
		}
	}

	/** Read-only probing of an unknown model. */
	static Model detect(RazerHidraw h) throws IOException {
		for (int tx : new int[] { 0x1f, 0x3f, 0xff }) {
			h.setTransactionId(tx);
			if (!h.send(0x04, 0x86, 0x26, new byte[] { 1 }, 1).ok())
				continue;
			int profiles = 5;
			var count = h.send(0x05, 0x8a, 0x08, new byte[0], 0);
			if (count.ok() && (count.args()[0] & 0xff) > 0 && (count.args()[0] & 0xff) <= 10)
				profiles = count.args()[0] & 0xff;
			var buttons = new LinkedHashMap<Integer, String[]>();
			for (int b = 0x01; b <= 0x6f; b++) {
				var r = h.send(0x02, 0x8c, 0x14, new byte[] { 1, (byte) b, 0 }, 2);
				if (r.ok() && (r.args()[3] != 0 || r.args()[4] != 0))
					buttons.put(b, new String[] { "Buttons", String.format("Button %02x", b) });
			}
			if (buttons.isEmpty())
				continue;
			var ledIds = new ArrayList<Integer>();
			for (int led : new int[] { 0x01, 0x04, 0x05, 0x0a, 0x0b }) {
				var r = h.sendMatching(0x0f, 0x82, 0x0c, new byte[] { 1, (byte) led }, new int[] { 1 });
				var br = h.send(0x0f, 0x84, 0x03, new byte[] { 1, (byte) led }, 2);
				System.getLogger(RazerOnboardProfiles.class.getName()).log(System.Logger.Level.DEBUG,
						String.format("LED %02x: effect %s, brightness %s", led, r.status(), br.status()));
				if (r.ok() && br.ok())
					ledIds.add(led);
			}
			return new Model(null, tx, profiles, buttons,
					leds(ledIds.stream().mapToInt(Integer::intValue).toArray()), false);
		}
		return null;
	}

	@Override
	public void close() {
		hid.close();
	}

	public Model getModel() {
		return model;
	}

	@Override
	public String getModelName() {
		return model.name();
	}

	@Override
	public boolean isVerified() {
		return model.verified();
	}

	@Override
	public boolean isExperimentalWrites() {
		return experimentalWrites;
	}

	@Override
	public void setExperimentalWrites(boolean experimentalWrites) {
		this.experimentalWrites = experimentalWrites;
	}

	@Override
	public int getProfileCount() {
		return model.profiles();
	}

	/** 05/80 = number of profiles in use, 05/81 = [slots, slot ids in cycle order...]. */
	@Override
	public List<Integer> getProfiles() throws IOException {
		var n = hid.send(0x05, 0x80, 0x08, new byte[0], 0);
		var list = hid.send(0x05, 0x81, 0x08, new byte[0], 0);
		if (n.ok() && list.ok()) {
			int c = n.args()[0] & 0xff;
			var l = new ArrayList<Integer>();
			for (int i = 0; i < c && i + 1 < 8; i++) {
				int id = list.args()[1 + i] & 0xff;
				if (id >= 1 && id <= getProfileCount() && !l.contains(id))
					l.add(id);
			}
			if (!l.isEmpty())
				return l;
		}
		return OnboardProfiles.super.getProfiles();
	}

	@Override
	public int getActiveProfile() throws IOException {
		return check(hid.send(0x05, 0x84, 0x08, new byte[0], 0), "read active profile").args()[0] & 0xff;
	}

	@Override
	public List<Integer> getButtonIds() {
		return List.copyOf(model.buttons().keySet());
	}

	@Override
	public String getButtonName(int buttonId) {
		var n = model.buttons().get(buttonId);
		return n == null ? String.format("Button %02x", buttonId) : n[1];
	}

	@Override
	public String getButtonGroup(int buttonId) {
		var n = model.buttons().get(buttonId);
		return n == null ? "Buttons" : n[0];
	}

	@Override
	public Action getButton(int profile, int buttonId) throws IOException {
		byte[] a = check(hid.send(0x02, 0x8c, 0x14, new byte[] { (byte) profile, (byte) buttonId, 0 }, 2),
				"read button").args();
		int len = Math.min(a[4] & 0xff, 15);
		return new Action(a[3] & 0xff, Arrays.copyOfRange(a, 5, 5 + len));
	}

	@Override
	public void setButton(int profile, int buttonId, Action action) throws IOException {
		checkWrite(profile);
		byte[] a = new byte[0x14];
		a[0] = (byte) profile;
		a[1] = (byte) buttonId;
		a[2] = 0; /* 1 here is acknowledged but ignored by the mouse */
		a[3] = (byte) action.type();
		a[4] = (byte) action.params().length;
		System.arraycopy(action.params(), 0, a, 5, Math.min(15, action.params().length));
		check(hid.send(0x02, 0x0c, 0x14, a, 2), "save button");
		Action now = getButton(profile, buttonId);
		if (now.type() != action.type() || !Arrays.equals(now.params(), action.params()))
			throw new IOException("The mouse did not keep the new button setting");
	}

	@Override
	public int getDpi(int profile) throws IOException {
		byte[] a = check(hid.send(0x04, 0x85, 0x07, new byte[] { (byte) profile }, 1), "read DPI").args();
		return (a[1] & 0xff) << 8 | (a[2] & 0xff);
	}

	@Override
	public void setDpi(int profile, int dpi) throws IOException {
		checkWrite(profile);
		checkDpi(dpi);
		byte h = (byte) (dpi >> 8), l = (byte) dpi;
		check(hid.send(0x04, 0x05, 0x07, new byte[] { (byte) profile, h, l, h, l, 0, 0 }, 1), "save DPI");
	}

	@Override
	public DpiStages getDpiStages(int profile) throws IOException {
		byte[] a = check(hid.send(0x04, 0x86, 0x26, new byte[] { (byte) profile }, 1), "read DPI stages").args();
		int n = Math.min(a[2] & 0xff, 5);
		int[] dpi = new int[n];
		int active = 0;
		for (int i = 0; i < n; i++) {
			int o = 3 + 7 * i;
			dpi[i] = (a[o + 1] & 0xff) << 8 | (a[o + 2] & 0xff);
			if ((a[o] & 0xff) == (a[1] & 0xff))
				active = i;
		}
		return new DpiStages(active, dpi);
	}

	@Override
	public void setDpiStages(int profile, DpiStages stages) throws IOException {
		checkWrite(profile);
		int n = Math.min(stages.dpi().length, 5);
		if (n < 1)
			throw new IllegalArgumentException("Need at least one DPI stage");
		byte[] a = new byte[0x26];
		a[0] = (byte) profile;
		a[1] = (byte) (Math.max(0, Math.min(n - 1, stages.active())) + 1);
		a[2] = (byte) n;
		for (int i = 0; i < n; i++) {
			int d = stages.dpi()[i];
			checkDpi(d);
			int o = 3 + 7 * i;
			a[o] = (byte) (i + 1);
			a[o + 1] = (byte) (d >> 8);
			a[o + 2] = (byte) d;
			a[o + 3] = (byte) (d >> 8);
			a[o + 4] = (byte) d;
		}
		check(hid.send(0x04, 0x06, 0x26, a, 1), "save DPI stages");
		var now = getDpiStages(profile);
		if (!Arrays.equals(now.dpi(), Arrays.copyOf(stages.dpi(), n)))
			throw new IOException("The mouse did not keep the new DPI stages");
	}

	@Override
	public List<Integer> getLedZones() {
		return List.copyOf(model.leds().keySet());
	}

	@Override
	public String getLedZoneName(int zone) {
		return model.leds().getOrDefault(zone, "LED " + zone);
	}

	@Override
	public Lighting getLighting(int profile, int zone) throws IOException {
		/* the reply always has 0 in byte 0, so only the LED id is echoed */
		byte[] a = check(hid.sendMatching(0x0f, 0x82, 0x0c, new byte[] { (byte) profile, (byte) zone }, new int[] { 1 }),
				"read lighting").args();
		int n = Math.min(a[5] & 0xff, 2);
		var cols = new ArrayList<int[]>();
		for (int i = 0; i < n; i++)
			cols.add(new int[] { a[6 + 3 * i] & 0xff, a[7 + 3 * i] & 0xff, a[8 + 3 * i] & 0xff });
		return new Lighting(a[2] & 0xff, a[3] & 0xff, a[4] & 0xff, cols);
	}

	@Override
	public void setLighting(int profile, int zone, Lighting lighting) throws IOException {
		checkWrite(profile);
		byte[] a = new byte[0x0c];
		a[0] = (byte) profile;
		a[1] = (byte) zone;
		a[2] = (byte) lighting.effect();
		a[3] = (byte) lighting.mode();
		a[4] = (byte) lighting.speed();
		int n = Math.min(lighting.colors().size(), 2);
		a[5] = (byte) n;
		for (int i = 0; i < n; i++)
			for (int c = 0; c < 3; c++)
				a[6 + 3 * i + c] = (byte) lighting.colors().get(i)[c];
		check(hid.sendMatching(0x0f, 0x02, 0x0c, a, new int[] { 1 }), "save lighting");
	}

	private void checkWrite(int profile) {
		if (!isWritable())
			throw new IllegalStateException(
					"Saving to this mouse has not been tested. Turn on experimental saving to allow it.");
		if (profile < 1 || profile > getProfileCount())
			throw new IllegalArgumentException("Profile must be 1-" + getProfileCount());
	}

	private static void checkDpi(int dpi) {
		if (dpi < 100 || dpi > 30000)
			throw new IllegalArgumentException("DPI must be 100-30000");
	}

	private static RazerHidraw.Reply check(RazerHidraw.Reply r, String what) throws IOException {
		if (!r.ok())
			throw new IOException("The mouse refused to " + what + " (status " + r.status() + ")");
		return r;
	}

	/**
	 * Read-only dump, for testing. Optional argument: product id in hex (default
	 * 0067). With "stages P a,b,c,d,e" writes DPI stages of profile P.
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && args[0].equals("detect")) {
			try (var h = RazerHidraw.open(VID, Integer.parseInt(args.length > 1 ? args[1] : "0067", 16))) {
				long t = System.currentTimeMillis();
				var m = detect(h);
				System.out.printf("detected in %d ms: %s%n", System.currentTimeMillis() - t, m == null ? "nothing"
						: String.format("tx %02x, %d profiles, buttons %s, leds %s", m.transactionId(), m.profiles(),
								m.buttons().keySet().stream().map(i -> String.format("%02x", i)).toList(), m.leds()));
			}
			return;
		}
		int pid = args.length > 0 && !args[0].equals("stages") ? Integer.parseInt(args[0], 16) : 0x0067;
		try (var t = open(pid)) {
			if (t == null) {
				System.out.println("Mouse not found or no onboard profiles");
				return;
			}
			if (args.length > 2 && args[0].equals("stages")) {
				int p = Integer.parseInt(args[1]);
				var before = t.getDpiStages(p);
				System.out.println("before " + Arrays.toString(before.dpi()) + " active " + (before.active() + 1));
				int[] d = Arrays.stream(args[2].split(",")).mapToInt(Integer::parseInt).toArray();
				t.setDpiStages(p, new DpiStages(before.active(), d));
				var after = t.getDpiStages(p);
				System.out.println("after  " + Arrays.toString(after.dpi()) + " active " + (after.active() + 1));
				return;
			}
			System.out.printf("%s, verified %s, %d slots, in use %s, active %d%n", t.getModelName(), t.isVerified(),
					t.getProfileCount(), t.getProfiles(), t.getActiveProfile());
			for (int p = 1; p <= t.getProfileCount(); p++) {
				var st = t.getDpiStages(p);
				System.out.printf("profile %d: dpi %d, stages %s (active %d)%n", p, t.getDpi(p),
						Arrays.toString(st.dpi()), st.active() + 1);
				for (int z : t.getLedZones()) {
					var l = t.getLighting(p, z);
					var cs = new StringBuilder();
					for (int[] c : l.colors())
						cs.append(String.format(" #%02x%02x%02x", c[0], c[1], c[2]));
					System.out.printf("  %s: effect %d mode %d speed %d%s%n", t.getLedZoneName(z), l.effect(),
							l.mode(), l.speed(), cs);
				}
				for (int b : t.getButtonIds()) {
					var a = t.getButton(p, b);
					System.out.printf("  %s %s: type %02x params %s%n", t.getButtonGroup(b), t.getButtonName(b),
							a.type(), Arrays.toString(a.params()));
				}
			}
		}
	}
}
