package uk.co.bithatch.snake.ui.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.PlatformService;

/**
 * Linux platform service for the fork.
 * <p>
 * Snake runs from its git checkout (see bin/snake). Updates come from the
 * fork's GitHub repository: the app checks whether origin/main has new
 * commits, and the launcher pulls, rebuilds and restarts. Settings shared with
 * the launcher live in ~/.config/snake/updates.
 */
public class LinuxPlatformService implements PlatformService {

	final static System.Logger LOG = System.getLogger(LinuxPlatformService.class.getName());

	private static final String SNAKE_RAZER_DESKTOP = "snake-razer.desktop";
	private static final String BRANCH = "main";

	private static final Object lock = new Object();
	private static boolean checked;
	private static String installedVersion = "Unknown";
	private static String availableVersion;
	private static int behind;
	private static Boolean updated;

	public LinuxPlatformService() {
		synchronized (lock) {
			if (!checked) {
				checked = true;
				installedVersion = describe("HEAD");
				ensureMenuEntry();
				if (isCheckForUpdates() && getRepo() != null) {
					Thread t = new Thread(this::checkForUpdates, "UpdateCheck");
					t.setDaemon(true);
					t.start();
				}
			}
		}
	}

	@Override
	public String getAvailableVersion() {
		return availableVersion == null ? getInstalledVersion() : availableVersion;
	}

	@Override
	public String getInstalledVersion() {
		return installedVersion;
	}

	@Override
	public boolean isBetas() {
		return false;
	}

	@Override
	public boolean isCheckForUpdates() {
		return !"false".equals(settings().getProperty("check"));
	}

	@Override
	public boolean isStartOnLogin() {
		File f = getAutostartFile();
		if (!f.exists())
			return false;
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("X-GNOME-Autostart-enabled=")) {
					return line.substring(26).equals("true");
				}
			}
		} catch (IOException ioe) {
		}
		return false;
	}

	@Override
	public boolean isUpdateableApp() {
		return getRepo() != null;
	}

	@Override
	public boolean isUpdateAutomatically() {
		return !"false".equals(settings().getProperty("auto"));
	}

	@Override
	public boolean isUpdateAvailable() {
		return behind > 0;
	}

	@Override
	public boolean isUpdated() {
		synchronized (lock) {
			if (updated == null) {
				/* The launcher leaves this marker after a successful update */
				Path marker = getCacheDir().resolve("updated");
				updated = Files.exists(marker);
				try {
					Files.deleteIfExists(marker);
				} catch (IOException e) {
				}
			}
			return updated;
		}
	}

	@Override
	public void setBetas(boolean betas) {
		/* No beta channel in the fork */
	}

	@Override
	public void setCheckForUpdates(boolean checkForUpdates) {
		saveSetting("check", String.valueOf(checkForUpdates));
	}

	@Override
	public void setStartOnLogin(boolean startOnLogin) throws IOException {
		if (isStartOnLogin() != startOnLogin)
			writeDesktopFile(getAutostartFile(), "Snake", "Control and configure your Razer devices", startOnLogin,
					"--no-open");
	}

	@Override
	public void setUpdateAutomatically(boolean updateAutomatically) {
		saveSetting("auto", String.valueOf(updateAutomatically));
	}

	@Override
	public void startUpdate() {
		/* Ask the launcher (bin/snake) to pull, rebuild and restart us */
		try {
			Path dir = Files.createDirectories(getCacheDir());
			Files.writeString(dir.resolve("action"), "update");
		} catch (IOException e) {
			LOG.log(Level.ERROR, "Could not request update.", e);
			return;
		}
		System.exit(0);
	}

	/**
	 * Make sure the app menu entry exists and launches this checkout.
	 */
	protected void ensureMenuEntry() {
		Path repo = getRepo();
		if (repo == null)
			return;
		File f = getShortcutFile();
		String exec = "Exec=" + repo.resolve("bin").resolve("snake");
		try {
			if (!f.exists() || !Files.readString(f.toPath()).contains(exec + "\n"))
				writeDesktopFile(f, "Snake", "Control and configure your Razer devices", null);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not write the app menu entry.", e);
		}
	}

	@Override
	public void addLaunchListener(Runnable listener) {
		/* bin/snake creates ~/.cache/snake/open when Snake is already running */
		Thread t = new Thread(() -> {
			try {
				Path dir = Files.createDirectories(getCacheDir());
				Path open = dir.resolve("open");
				Files.deleteIfExists(open);
				try (java.nio.file.WatchService ws = dir.getFileSystem().newWatchService()) {
					dir.register(ws, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
							java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
					while (true) {
						java.nio.file.WatchKey key = ws.take();
						boolean requested = false;
						for (java.nio.file.WatchEvent<?> ev : key.pollEvents()) {
							if (open.getFileName().equals(ev.context()))
								requested = true;
						}
						key.reset();
						if (requested && Files.deleteIfExists(open))
							listener.run();
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Cannot watch for launch requests.", e);
			}
		}, "LaunchListener");
		t.setDaemon(true);
		t.start();
	}

	protected void checkForUpdates() {
		if (git("fetch", "-q", "origin", BRANCH) == null) {
			LOG.log(Level.INFO, "Could not check for updates (offline?).");
			return;
		}
		String count = git("rev-list", "--count", "HEAD..origin/" + BRANCH);
		try {
			behind = count == null ? 0 : Integer.parseInt(count.trim());
		} catch (NumberFormatException nfe) {
			behind = 0;
		}
		if (behind > 0) {
			availableVersion = describe("origin/" + BRANCH);
			LOG.log(Level.INFO, String.format("Update available: %s (%d new commits)", availableVersion, behind));
		}
	}

	protected String describe(String ref) {
		String v = git("log", "-1", "--format=%h (%cs)", ref);
		return v == null ? "Unknown" : v.trim();
	}

	/**
	 * Run git in the checkout, returning stdout or null on failure.
	 */
	protected String git(String... args) {
		Path repo = getRepo();
		if (repo == null)
			return null;
		List<String> cmd = new ArrayList<>();
		cmd.add("git");
		cmd.add("-C");
		cmd.add(repo.toString());
		for (String a : args)
			cmd.add(a);
		try {
			Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
			p.getOutputStream().close();
			String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return null;
			}
			return p.exitValue() == 0 ? out : null;
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	/**
	 * The git checkout Snake is running from (it is started from
	 * snake-app-linux inside it), or null.
	 */
	protected Path getRepo() {
		String prop = System.getProperty("snake.repo");
		Path p = prop == null ? Paths.get(System.getProperty("user.dir")).toAbsolutePath() : Paths.get(prop);
		while (p != null) {
			if (Files.exists(p.resolve(".git")) && Files.exists(p.resolve("bin").resolve("snake")))
				return p;
			p = p.getParent();
		}
		return null;
	}

	protected Path getCacheDir() {
		String xdg = System.getenv("XDG_CACHE_HOME");
		Path base = xdg == null || xdg.isEmpty() ? Paths.get(System.getProperty("user.home"), ".cache") : Paths.get(xdg);
		return base.resolve("snake");
	}

	protected Path getSettingsFile() {
		String xdg = System.getenv("XDG_CONFIG_HOME");
		Path base = xdg == null || xdg.isEmpty() ? Paths.get(System.getProperty("user.home"), ".config")
				: Paths.get(xdg);
		return base.resolve("snake").resolve("updates");
	}

	protected Properties settings() {
		Properties p = new Properties();
		Path f = getSettingsFile();
		if (Files.exists(f)) {
			try (InputStream in = Files.newInputStream(f)) {
				p.load(in);
			} catch (IOException e) {
			}
		}
		return p;
	}

	protected void saveSetting(String key, String value) {
		Properties p = settings();
		p.setProperty(key, value);
		Path f = getSettingsFile();
		try {
			Files.createDirectories(f.getParent());
			/* Plain key=value lines so the shell launcher can read them */
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(f))) {
				for (String k : p.stringPropertyNames())
					pw.println(k + "=" + p.getProperty(k));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to save update setting.", e);
		}
	}

	File getAutostartFile() {
		return new File(System.getProperty("user.home") + File.separator + ".config" + File.separator + "autostart"
				+ File.separator + SNAKE_RAZER_DESKTOP);
	}

	File getShare() {
		return new File(System.getProperty("user.home") + File.separator + ".local" + File.separator + "share");
	}

	File getShortcutFile() {
		return new File(getShare(), "applications" + File.separator + SNAKE_RAZER_DESKTOP);
	}

	private File checkFilesParent(File file) throws IOException {
		if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new IOException(String.format("Failed to create parent folder for %s.", file));
		}
		return file;
	}

	private void writeDesktopFile(File file, String name, String comment, Boolean autoStart, String... execArgs)
			throws IOException, FileNotFoundException {
		Path repo = getRepo();
		if (repo == null)
			throw new IOException("Cannot find the Snake checkout to launch from.");
		checkFilesParent(file);
		try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
			pw.println("#!/usr/bin/env xdg-open");
			pw.println("[Desktop Entry]");
			pw.println("Version=1.0");
			pw.println("Terminal=false");
			File iconFile = checkFilesParent(new File(getShare(), "pixmaps" + File.separator + "snake-razer.png"));
			try (FileOutputStream fos = new FileOutputStream(iconFile)) {
				try (InputStream in = App.class.getResourceAsStream("icons/app512.png")) {
					in.transferTo(fos);
				}
			}
			pw.println("Icon=" + iconFile.getAbsolutePath());
			pw.println("Exec=" + repo.resolve("bin").resolve("snake")
					+ (execArgs.length == 0 ? "" : " " + String.join(" ", execArgs)));
			pw.println("Name=" + name);
			pw.println("Comment=" + comment);
			pw.println("Categories=Utility;");
			pw.println("StartupNotify=false");
			pw.println("Type=Application");
			pw.println("Keywords=razer;snake;mouse;chroma;naga");
			if (autoStart != null) {
				pw.println("X-GNOME-Autostart-enabled=" + autoStart);
			}
		}
	}
}
