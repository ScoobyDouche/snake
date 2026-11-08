package uk.co.bithatch.macrolib;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import com.sshtools.jfreedesktop.icons.IconService;
import com.sshtools.jfreedesktop.icons.LinuxIconService;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice.Event;
import uk.co.bithatch.macrolib.WindowMonitor.Listener;

/**
 * The Class MacroSystem.
 */
public class MacroSystem implements AutoCloseable, ActionListener {

	/**
	 * The listener interface for receiving activeBank events. The class that is
	 * interested in processing a activeBank event implements this interface, and
	 * the object created with that class is registered with a component using the
	 * component's <code>addActiveBankListener</code> method. When the activeBank
	 * event occurs, that object's appropriate method is invoked.
	 */
	public interface ActiveBankListener {

		/**
		 * Active bank changed.
		 *
		 * @param device the device
		 * @param bank   the bank
		 */
		void activeBankChanged(MacroDevice device, MacroBank bank);
	}

	/**
	 * The listener interface for receiving activeProfile events. The class that is
	 * interested in processing a activeProfile event implements this interface, and
	 * the object created with that class is registered with a component using the
	 * component's <code>addActiveProfileListener</code> method. When the
	 * activeProfile event occurs, that object's appropriate method is invoked.
	 */
	public interface ActiveProfileListener {

		/**
		 * Active profile changed.
		 *
		 * @param device  the device
		 * @param profile the profile
		 */
		void activeProfileChanged(MacroDevice device, MacroProfile profile);
	}

	/**
	 * The listener interface for receiving macro system events. The class that is
	 * interested in processing a macro system event implements this interface, and
	 * the object created with that class is registered with a component using the
	 * component's <code>addMacroSystemListener</code> method. When the macro system
	 * event occurs, that object's appropriate method is invoked.
	 */
	public interface MacroSystemListener {

		/**
		 * Macro system changed.
		 */
		void macroSystemChanged();
	}

	/**
	 * The listener interface for receiving profile events. The class that is
	 * interested in processing a profile event implements this interface, and the
	 * object created with that class is registered with a component using the
	 * component's <code>addProfileListener</code> method. When the profile event
	 * occurs, that object's appropriate method is invoked.
	 */
	public interface ProfileListener {

		/**
		 * Profile changed.
		 *
		 * @param device  the device
		 * @param profile the profile
		 */
		void profileChanged(MacroDevice device, MacroProfile profile);
	}

	/**
	 * Interface to be implemented by classes wishing to be notified of events in
	 * the macro recording system.
	 */
	public interface RecordingListener {

		/**
		 * An event was recorded
		 */
		void eventRecorded();

		/**
		 * Macro system state changed.
		 * @param session TODO
		 */
		void recordingStateChange(RecordingSession session);
	}

	static class ApplicationMatch {
		List<Pattern> exclude = new ArrayList<>();
		UUID id;
		List<Pattern> include = new ArrayList<>();
		private MacroProfile profile;

		ApplicationMatch(MacroProfile profile) {
			this.profile = profile;
			id = profile.getId();
			for (String inc : profile.getIncludeApplications()) {
				include.add(Pattern.compile(inc));
			}
			for (String exc : profile.getExcludeApplications()) {
				exclude.add(Pattern.compile(exc));
			}
		}

		boolean matches(Application app) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Matching %s against %s and %s in %s", app.getName(), include,
						exclude, profile.getId()));
			if (include.isEmpty() && exclude.isEmpty()) {
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("   Not matching because no rules"));
				return false;
			} else {
				boolean res = (include.isEmpty() || matches(include, app)) && !matches(exclude, app);
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("   Match = %s", res));
				return res;
			}
		}

		boolean matches(List<Pattern> l, Application app) {
			for (Pattern p : l) {
				if (p.matcher(app.getName()).matches())
					return true;
			}
			return false;
		}
	}

	static class MacroDeviceState implements Closeable {
		List<MacroProfile> activeProfiles = new ArrayList<>();
		MacroBank bank;
		MacroBank defaultBank;
		MacroProfile defaultProfile;
		MacroDevice device;
		DeviceHandler handler;
		MacroKeyboard keyboard;
		List<ApplicationMatch> matches = new ArrayList<>();

		@Override
		public void close() throws IOException {
			keyboard.close();
			device.close();
			handler.close();
		}

		public MacroProfile getActiveProfile() {
			return activeProfiles.isEmpty() ? null : activeProfiles.get(0);
		}

		public UUID match(Application activeApplication) {
			for (ApplicationMatch m : matches) {
				if (m.matches(activeApplication))
					return m.id;
			}
			return null;
		}

		public void open() {
			device.open(handler);
		}
	}

	/** The Constant ACTION_BANK_0. */
	public static final String ACTION_BANK_0 = "bank-0";

	/** The Constant ACTION_BANK_1. */
	public static final String ACTION_BANK_1 = "bank-1";

	/** The Constant ACTION_BANK_2. */
	public static final String ACTION_BANK_2 = "bank-2";

	/** The Constant ACTION_BANK_3. */
	public static final String ACTION_BANK_3 = "bank-3";

	/** The Constant ACTION_BANK_4. */
	public static final String ACTION_BANK_4 = "bank-4";

	/** The Constant ACTION_BANK_5. */
	public static final String ACTION_BANK_5 = "bank-5";

	/** The Constant ACTION_BANK_6. */
	public static final String ACTION_BANK_6 = "bank-6";

	/** The Constant ACTION_BANK_7. */
	public static final String ACTION_BANK_7 = "bank-7";

	/** The Constant ACTION_BANK_8. */
	public static final String ACTION_BANK_8 = "bank-8";

	/** The Constant ACTION_BANK_9. */
	public static final String ACTION_BANK_9 = "bank-9";

	/** The Constant ACTION_CYCLE_BANK. */
	public static final String ACTION_CYCLE_BANK = "cycle-bank";

	/** The Constant ACTION_NEXT_BANK. */
	public static final String ACTION_NEXT_BANK = "next-bank";

	/** The Constant ACTION_PREVIOUS_BANK. */
	public static final String ACTION_PREVIOUS_BANK = "previous-bank";

	/** The Constant ACTION_START_RECORDING. */
	public static final String ACTION_START_RECORDING = "start-recording";

	/** The Constant ACTION_START_RECORDING. */
	public static final String ACTION_STOP_RECORDING = "stop-recording";

	/** The Constant ACTION_TOGGLE_RECORDING_PAUSE. */
	public static final String ACTION_TOGGLE_PAUSE_RECORDING = "toggle-pause-recording";

	/** The Constant DEFAULT_UID. */
	public final static UUID DEFAULT_UID = new UUID(0, 0);

	final static Logger LOG = System.getLogger(MacroKeyboard.class.getName());

	/**
	 * Generate profile id.
	 *
	 * @return the string
	 */
	public static String generateProfileId() {
		return UUID.randomUUID().toString();
	}

	private Map<String, Action> actions = new HashMap<>();
	private List<ActiveBankListener> activeBankListeners = new ArrayList<>();
	private List<ActiveProfileListener> activeProfileListeners = new ArrayList<>();
	private DesktopIO desktopIO;
	private Map<MacroDevice, MacroDeviceState> devices = Collections.synchronizedMap(new HashMap<>());
	private IconService iconService;
	private double keyHoldDelay = 2;
	private ScheduledExecutorService macroQueue;
	private List<MacroSystemListener> macroSystemListeners = new ArrayList<>();
	private WindowMonitor monitor;
	private boolean open;
	private List<ProfileListener> profileListeners = new ArrayList<>();
	private ScheduledExecutorService queue;
	private List<RecordingListener> recordingListeners = new ArrayList<>();
	private RecordingSession recordingSession = new RecordingSession();
	private MacroStorage storage;
	private UInput uinput;

	/**
	 * Instantiates a new macro system.
	 */
	public MacroSystem() {
		this(new JsonMacroStorage());
	}

	/**
	 * Instantiates a new macro system.
	 */
	public MacroSystem(MacroStorage storage) {
		this.storage = storage;

		/* Fast queue for timers */
		queue = Executors.newScheduledThreadPool(1);

		/* Separate queue for possibly long running scripts etc */
		macroQueue = Executors.newScheduledThreadPool(1);
		desktopIO = new X11DesktopIO();

		/* Initialise storage */
		this.storage.init(this);

		try {
			iconService = new LinuxIconService();
			iconService.postInit();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to start icon service.", e);
		}
		uinput = new UInput(this);

		/* Next bank */
		addAction(new Action(ACTION_CYCLE_BANK, (ex) -> {
			MacroBank bank = getActiveBank(ex.getDevice());
			int bankNo = bank.getBank();
			MacroProfile profile = bank.getProfile();
			if (bankNo >= ex.getDevice().getBanks() - 1)
				bankNo = 0;
			else
				bankNo++;
			bank = profile.getBank(bankNo);
			try {
				setActiveBank(bank);
				return true;
			} catch (IOException e) {
				LOG.log(Level.ERROR, "Failed to switch bank.", e);
			}
			return false;
		}));

		/* Next bank */
		addAction(new Action(ACTION_NEXT_BANK, (ex) -> {
			MacroBank bank = getActiveBank(ex.getDevice());
			int bankNo = bank.getBank();
			MacroProfile profile = bank.getProfile();
			if (bankNo < ex.getDevice().getBanks()) {
				bank = profile.getBank(bankNo + 1);
				try {
					setActiveBank(bank);
					return true;
				} catch (IOException e) {
					LOG.log(Level.ERROR, "Failed to switch bank.", e);
				}
			}
			return false;
		}));

		/* Previous bank */
		addAction(new Action(ACTION_PREVIOUS_BANK, (ex) -> {
			MacroBank bank = getActiveBank(ex.getDevice());
			int bankNo = bank.getBank();
			MacroProfile profile = bank.getProfile();
			if (bankNo > 0) {
				bank = profile.getBank(bankNo - 1);
				try {
					setActiveBank(bank);
					return true;
				} catch (IOException e) {
					LOG.log(Level.ERROR, "Failed to switch bank.", e);
				}
			}
			return false;
		}));

		/* Previous bank */
		for (int i = 0; i < 10; i++) {
			int fi = i;
			addAction(new Action("bank-" + i, (ex) -> {
				try {
					MacroBank bank = getActiveProfile(ex.getDevice()).getBank(fi);
					setActiveBank(bank);
					return true;
				} catch (IOException e) {
					LOG.log(Level.ERROR, "Failed to switch bank.", e);
				}
				return false;
			}));
		}

		/* Start recording */
		addAction(new Action(ACTION_START_RECORDING, (ex) -> {
			startRecording();
			return true;
		}));

		/* Stop recording */
		addAction(new Action(ACTION_STOP_RECORDING, (ex) -> {
			stopRecording();
			return true;
		}));

		/* Toggle pause recording  */
		addAction(new Action(ACTION_TOGGLE_PAUSE_RECORDING, (ex) -> {
			togglePauseRecording();
			return true;
		}));
	}

	/**
	 * Action performed.
	 *
	 * @param binding the binding
	 * @return true, if successful
	 */
	@Override
	public boolean actionPerformed(ActionBinding binding) {
		Action action = actions.get(binding.getAction());
		if (action != null) {
			return action.actionPerformed(binding);
		}
		return false;
	}

	/**
	 * Adds the action.
	 *
	 * @param action the action
	 */
	public void addAction(Action action) {
		if (actions.containsKey(action.getId()))
			throw new IllegalArgumentException(String.format("Action %s already registered.", action.getId()));
		this.actions.put(action.getId(), action);
	}

	/**
	 * Add a listener that is notified when the active bank changes on any device.
	 * 
	 * @param listener listener to be notified when any devices active bank changes
	 */
	public void addActiveBankListener(ActiveBankListener listener) {
		this.activeBankListeners.add(listener);
	}

	/**
	 * Add a listener that is notified when the active activeProfiles changes on any
	 * device.
	 * 
	 * @param listener listener to be notified when any devices active
	 *                 activeProfiles changes
	 */
	public void addActiveProfileListener(ActiveProfileListener listener) {
		this.activeProfileListeners.add(listener);
	}

	/**
	 * Add a new device to those managed by the macro system. At this point, the
	 * device will be 'grabbed', and all further events that come from it will be
	 * routed through the macro system.
	 * 
	 * When a device is first added, it's default activeProfiles will be created.
	 *
	 * @param device device to add
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void addDevice(MacroDevice device) throws IOException {
		synchronized (devices) {
			MacroDeviceState macroDeviceState = new MacroDeviceState();
			devices.put(device, macroDeviceState);
			macroDeviceState.device = device;

			MacroProfile profile = loadActiveProfile(device);
			macroDeviceState.activeProfiles.add(profile);
			macroDeviceState.bank = profile.getBank(storage.loadActiveBank(profile));

			UUID defaultUUID = storage.loadDefaultProfile(device);
			if (defaultUUID == null) {
				storage.setDefaultProfile(profile);
				defaultUUID = profile.getId();
			}

			if (defaultUUID.equals(profile.getId())) {
				macroDeviceState.defaultProfile = profile;
			} else {
				macroDeviceState.defaultProfile = storage.loadProfile(device, defaultUUID);
				if (macroDeviceState.defaultProfile == null) {
					storage.setDefaultProfile(profile);
					macroDeviceState.defaultProfile = profile;
				}
			}

			int defaultBank = storage.loadDefaultBank(macroDeviceState.defaultProfile);
			if (defaultBank == -1) {
				storage.setDefaultBank(profile, 0);
				defaultBank = 0;
			}
			macroDeviceState.defaultBank = macroDeviceState.defaultProfile.getBank(defaultBank);

			MacroKeyboard keyboard = new MacroKeyboard(this, device, queue);
			keyboard.addActionListener(this);
			DeviceHandler handler = new ForwardDeviceHandler(uinput, device, (key, state, event) -> {
				/* Intercept the event consume so we can capture during recording */
				if (processForRecording(key, state, event, device))
					keyboard.keyReceived(key, state, event);
			}, queue);
			macroDeviceState.handler = handler;
			macroDeviceState.keyboard = keyboard;

			/*
			 * Build the map of regular expression patterns for all the profiles the device
			 * has that activate when certain windows become active. This is so we can keep
			 * with the principal that only one active profile is every loaded into memory at
			 * a time for any device.
			 * 
			 */
			for (Iterator<MacroProfile> profileIt = storage.profiles(device); profileIt.hasNext();) {
				profile = profileIt.next();
				if (profile.isActivatedByApplication()) {
					macroDeviceState.matches.add(new ApplicationMatch(profile));
				}
			}

			if (open)
				macroDeviceState.open();
		}
	}

	/**
	 * Add a listener that is notified when profiles are added or removed, or other
	 * global macro system changes.
	 * 
	 * @param listener listener to be notified when macro system changes
	 */
	public void addMacroSystemListener(MacroSystemListener listener) {
		this.macroSystemListeners.add(listener);
	}

	/**
	 * Add a listener that is notified when a activeProfiles changes on any device.
	 * 
	 * @param listener listener to be notified when activeProfiles changes
	 */
	public void addProfileListener(ProfileListener listener) {
		this.profileListeners.add(listener);
	}

	/**
	 * Add a listener that is notified when events happen in the macro recording
	 * system.
	 * 
	 * @param listener listener to be notified when recording state changes
	 */
	public void addRecordingListener(RecordingListener listener) {
		this.recordingListeners.add(listener);
	}

	/**
	 * Close.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void close() throws Exception {
		storage.close();
		monitor.close();
		uinput.close();
		synchronized (devices) {
			for (MacroDeviceState device : devices.values()) {
				device.close();
			}
			devices.clear();
		}
		queue.shutdown();
		macroQueue.shutdown();
	}

	/**
	 * Creates the profile.
	 *
	 * @param device the device
	 * @return the macro profile
	 */
	public MacroProfile createProfile(MacroDevice device, String name) {
		MacroProfile p = doCreateProfile(device, name);
		fireMacroSystemChanged();
		return p;
	}

	/**
	 * Gets the actions.
	 *
	 * @return the actions
	 */
	public Map<String, Action> getActions() {
		return Collections.unmodifiableMap(actions);
	}

	/**
	 * Get the currently active bank for the specified device. This will be between
	 * 1 and {@link MacroDevice#getBanks()}.
	 *
	 * @param device device associated with activeProfiles
	 * @return bank
	 */
	public MacroBank getActiveBank(MacroDevice device) {
		checkDevice(device);
		return devices.get(device).bank;
	}

	/**
	 * Get the currently active activeProfiles for the specified device.
	 *
	 * @param device device associated with activeProfiles
	 * @return activeProfiles
	 */
	public MacroProfile getActiveProfile(MacroDevice device) {
		checkDevice(device);
		return devices.get(device).getActiveProfile();
	}

	/**
	 * Gets the default bank.
	 *
	 * @param device the device
	 * @return the default bank
	 */
	public MacroBank getDefaultBank(MacroDevice device) {
		checkDevice(device);
		return devices.get(device).defaultBank;
	}

	/**
	 * Gets the default profile.
	 *
	 * @param device the device
	 * @return the default profile
	 */
	public MacroProfile getDefaultProfile(MacroDevice device) {
		checkDevice(device);
		return devices.get(device).defaultProfile;
	}

	/**
	 * Gets the desktop IO.
	 *
	 * @return the desktop IO
	 */
	public DesktopIO getDesktopIO() {
		return desktopIO;
	}

	/**
	 * Gets the icon service.
	 *
	 * @return the icon service
	 */
	public IconService getIconService() {
		return iconService;
	}

	/**
	 * Get how long in seconds a key should be held down before before it triggers
	 * the {@link KeyState#HELD} state.
	 * 
	 * @return key hold delay
	 */
	public double getKeyHoldDelay() {
		return keyHoldDelay;
	}

	/**
	 * Gets the macro queue.
	 *
	 * @return the macro queue
	 */
	public ScheduledExecutorService getMacroQueue() {
		return macroQueue;
	}

	/**
	 * Get the number of profiles a device has.
	 * 
	 * @param device device
	 * @return number of profiles
	 */
	public int getNumberOfProfiles(MacroDevice device) {
		try {
			return storage.getNumberOfProfiles(device);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to count profiles for device %s", device.getId()));
		}
	}

	/**
	 * Gets the or create profile with name.
	 *
	 * @param device the device
	 * @param name   the name
	 * @return the or create profile with name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public MacroProfile getOrCreateProfileWithName(MacroDevice device, String name) throws IOException {
		MacroProfile profile = getProfileWithName(device, name);
		if (profile == null) {
			profile = doCreateProfile(device, name);
			profile.setName(name);
			storage.saveProfile(profile);
			fireMacroSystemChanged();
		}
		return profile;
	}

	/**
	 * Gets the profile with name.
	 *
	 * @param device the device
	 * @param name   the name
	 * @return the profile with name
	 */
	public MacroProfile getProfileWithName(MacroDevice device, String name) {
		try {
			return storage.getProfileWithName(device, name);
		} catch (IOException e) {
			throw new IllegalStateException(
					String.format("Failed to get a profile given it's name of %s on device %s.", name, device.getId()));
		}
	}

	/**
	 * Get recording session.
	 * 
	 * @return recording session
	 */
	public RecordingSession getRecordingSession() {
		return recordingSession;
	}

	/**
	 * Gets the u input.
	 *
	 * @return the u input
	 */
	public UInput getUInput() {
		return uinput;
	}

	/**
	 * Gets the writer.
	 *
	 * @return the writer
	 */
	public MacroStorage getWriter() {
		return storage;
	}

	/**
	 * Get the window monitor.
	 * 
	 * @return window monitor
	 */
	public WindowMonitor getMonitor() {
		return monitor;
	}

	/**
	 * Get if the active activeProfiles is "locked" or if it may be changed for the
	 * specified device.
	 * 
	 * device device associated with activeProfiles
	 *
	 * @param device the device
	 * @return active
	 */
	public boolean isLocked(MacroDevice device) {
		return storage.isLocked(device);
	}

	/**
	 * Checks if is open.
	 *
	 * @return true, if is open
	 */
	public boolean isOpen() {
		return open;
	}

	/**
	 * Get if recording.
	 * 
	 * @return recording
	 */
	public boolean isRecording() {
		synchronized (recordingSession) {
			return recordingSession.getRecordingState().isRecording();
		}
	}

	/**
	 * Open.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void open() throws IOException {
		if (open)
			throw new IllegalStateException("Already open.");
		uinput.open();
		open = true;

		for (MacroDeviceState device : devices.values())
			device.open();

		monitor = new WindowMonitor(queue);
		monitor.addListener(new Listener() {

			@Override
			public void activeChanged(Application oldApp, Application newApp) {
				checkActiveApp(newApp);
			}

			@Override
			public void activeChanged(Window oldWindow, Window newWindow) {
			}

			@Override
			public void viewClosed(View view) {
			}

			@Override
			public void viewOpened(View view) {
			}

		});
		checkActiveApp(monitor.getActiveApplication());
	}

	/**
	 * Profiles.
	 *
	 * @param device the device
	 * @return the iterator
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Iterator<MacroProfile> profiles(MacroDevice device) throws IOException {
		return storage.profiles(device);
	}

	/**
	 * Removes the action.
	 *
	 * @param action the action
	 */
	public void removeAction(Action action) {
		this.actions.remove(action.getId());
	}

	/**
	 * Remove a listener from those notified when the active bank changes on any
	 * device.
	 * 
	 * @param listener listener to be removed from the list of those notified when
	 *                 any devices active bank changes
	 */
	public void removeActiveBankListener(ActiveBankListener listener) {
		this.activeBankListeners.remove(listener);
	}

	/**
	 * Remove a listener from those notified when the active activeProfiles changes
	 * on any device.
	 * 
	 * @param listener listener to be removed from the list of those notified when
	 *                 any devices active activeProfiles changes
	 */
	public void removeActiveProfileListener(ActiveProfileListener listener) {
		this.activeProfileListeners.remove(listener);
	}

	/**
	 * Remove a new device from those managed by the macro system. At this point,
	 * the device will be 'ungrabbed', and all further events that come from it will
	 * be routed normally.
	 *
	 * @param device device to remove
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void removeDevice(MacroDevice device) throws IOException {
		checkDevice(device);
		MacroDeviceState state = devices.remove(device);
		if (state != null) {
			state.keyboard.removeActionListener(this);
			state.keyboard.close();
		}
		device.close();
	}

	/**
	 * Remove a listener from those notified when profiles are added or removed, or
	 * other global macro system changes.
	 * 
	 * @param listener listener to no longer be notified when macro system changes
	 */
	public void removeMacroSystemListener(MacroSystemListener listener) {
		this.macroSystemListeners.remove(listener);
	}

	/**
	 * Remove a listener from those notified when the active activeProfiles changes
	 * on any device.
	 * 
	 * @param listener listener to be removed from the list of those notified when
	 *                 any activeProfiles changes
	 */
	public void removeProfileListener(ProfileListener listener) {
		this.profileListeners.remove(listener);
	}

	/**
	 * Remove a listener from those notified when events happen in the macro
	 * recording system.
	 * 
	 * @param listener listener to be removed
	 */
	public void removeRecordingListener(RecordingListener listener) {
		this.recordingListeners.remove(listener);
	}

	/**
	 * Set the bank active for a particular device.
	 * 
	 * @param bank bank
	 * @throws IOException on any error
	 */
	public void setActiveBank(MacroBank bank) throws IOException {
		MacroProfile bankProfile = bank.getProfile();
		if (!bank.equals(getActiveBank(bankProfile.getDevice()))) {
			MacroDevice device = bankProfile.getDevice();
			checkDevice(device);
			MacroProfile activeProfile = getActiveProfile(device);
			if (Objects.equals(activeProfile, bankProfile)) {
				LOG.log(Level.INFO,
						String.format("Setting device %s to bank %d", bankProfile.getDevice().getId(), bank.getBank()));
				devices.get(device).bank = bank;
				storage.setActiveBank(bank);
				for (int i = activeBankListeners.size() - 1; i >= 0; i--)
					activeBankListeners.get(i).activeBankChanged(device, bank);
			} else {
				LOG.log(Level.INFO, String.format("Setting device %s to profile %s, bank %s",
						bankProfile.getDevice().getId(), bankProfile.getName(), bank.getDisplayName()));

				MacroDeviceState state = devices.get(device);
				state.activeProfiles.clear();
				state.activeProfiles.add(bankProfile);
				state.bank = bankProfile.getBank(storage.loadActiveBank(bankProfile));
				for (int i = activeProfileListeners.size() - 1; i >= 0; i--)
					activeProfileListeners.get(i).activeProfileChanged(device, bankProfile);
			}
		}
	}

	/**
	 * Set the activeProfiles active for a particular device.
	 *
	 * @param macroProfile activeProfiles to set
	 * @throws IOException on any error
	 */
	public void setActiveProfile(MacroProfile macroProfile) throws IOException {
		if (!macroProfile.equals(getActiveProfile(macroProfile.getDevice()))) {
			MacroDevice device = macroProfile.getDevice();
			checkDevice(device);
			if (isLocked(macroProfile.getDevice()))
				throw new IllegalStateException("Profile is locked on this device.");

			LOG.log(Level.INFO, String.format("Setting device %s to profile %s", macroProfile.getDevice().getId(),
					macroProfile.getName()));
			storage.setActiveProfile(macroProfile);
			MacroDeviceState state = devices.get(device);
			state.activeProfiles.clear();
			state.activeProfiles.add(macroProfile);
			state.bank = macroProfile.getBank(storage.loadActiveBank(macroProfile));
			for (int i = activeProfileListeners.size() - 1; i >= 0; i--)
				activeProfileListeners.get(i).activeProfileChanged(device, macroProfile);
		}
	}

	/**
	 * Set the default bank for a particular profile.
	 *
	 * @param macroBank the new default bank
	 * @throws IOException on any error
	 */
	public void setDefaultBank(MacroBank macroBank) throws IOException {
		MacroDevice device = macroBank.getProfile().getDevice();
		checkDevice(device);
		storage.setDefaultBank(macroBank.getProfile(), macroBank.getBank());
		devices.get(device).defaultBank = macroBank;
		for (int i = profileListeners.size() - 1; i >= 0; i--)
			profileListeners.get(i).profileChanged(device, macroBank.getProfile());
	}

	/**
	 * Set the default profile for a particular device.
	 *
	 * @param macroProfile the new default profile
	 */
	public void setDefaultProfile(MacroProfile macroProfile) {
		MacroDevice device = macroProfile.getDevice();
		checkDevice(device);
		try {
			storage.setDefaultProfile(macroProfile);
			devices.get(device).defaultProfile = macroProfile;
			for (int i = profileListeners.size() - 1; i >= 0; i--)
				profileListeners.get(i).profileChanged(device, macroProfile);
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to set default profile.", ioe);
		}
	}

	/**
	 * Sets the desktop IO.
	 *
	 * @param desktopIO the new desktop IO
	 */
	public void setDesktopIO(DesktopIO desktopIO) {
		this.desktopIO = desktopIO;
	}

	/**
	 * Set how long in seconds a key should be held down before before it triggers
	 * the {@link KeyState#HELD} state.
	 *
	 * @param keyHoldDelay the new key hold delay
	 */
	public void setKeyHoldDelay(double keyHoldDelay) {
		this.keyHoldDelay = keyHoldDelay;
	}

	/**
	 * 
	 * Set if the active activeProfiles is 'locked', or if it may be changed for the
	 * specified device.
	 * 
	 * @param device device associated with activeProfiles
	 * @param locked lock status
	 * @throws IOException on error
	 */
	public void setLocked(MacroDevice device, boolean locked) throws IOException {
		storage.setLocked(device, locked);
	}

	/**
	 * Start recording
	 */
	public void startRecording() {
		synchronized (recordingSession) {
			if (recordingSession.getRecordingState() != RecordingState.IDLE
					&& recordingSession.getRecordingState() != RecordingState.ERROR) {
				throw new IllegalStateException();
			}
			recordingSession.start();
			for (int i = recordingListeners.size() - 1; i >= 0; i--)
				recordingListeners.get(i).recordingStateChange(recordingSession);
		}
	}

	/**
	 * Stop recording
	 */
	public void stopRecording() {
		synchronized (recordingSession) {
			if (recordingSession.getRecordingState() == RecordingState.IDLE
					|| recordingSession.getRecordingState() == RecordingState.ERROR) {
				throw new IllegalStateException();
			}
			recordingSession.stop();
			for (int i = recordingListeners.size() - 1; i >= 0; i--)
				recordingListeners.get(i).recordingStateChange(recordingSession);
		}
	}

	/**
	 * Toggle pause recording
	 */
	public void togglePauseRecording() {
		synchronized (recordingSession) {
			if (!isRecording()) {
				throw new IllegalStateException();
			}
			if (recordingSession.getRecordingState() == RecordingState.PAUSED)
				recordingSession.unpause();
			else
				recordingSession.pause();
			for (int i = recordingListeners.size() - 1; i >= 0; i--)
				recordingListeners.get(i).recordingStateChange(recordingSession);
		}
	}

	protected void checkActiveApp(Application activeApplication) {
		if (activeApplication == null)
			return;
		queue.execute(() -> {
			synchronized (devices) {
				for (MacroDeviceState state : devices.values()) {
					if (LOG.isLoggable(Level.DEBUG))
						LOG.log(Level.DEBUG, String.format("Checking profiles for application '%s' on %s. Icon %s",
								activeApplication.getName(), state.device.getId(), activeApplication.getIcon()));
					UUID profile = state.match(activeApplication);
					if (profile != null) {

						try {
							pushActiveProfile(storage.loadProfile(state.device, profile));
						} catch (IOException e) {
							LOG.log(Level.ERROR, "Failed to switch profiles.", e);
						}
						;
						break;
					} else {
						/*
						 * The new application doesn't match any profiles. If the last activeProfiles
						 * was switch to automatically, then revert to the previous one.
						 */
						if (state.activeProfiles.size() > 1) {
							state.activeProfiles.remove(0);
							MacroProfile newProfile = state.activeProfiles.get(0);
							LOG.log(Level.INFO, String.format("Returning to profile %s (%s)", newProfile.getId(),
									newProfile.getName()));
							for (int i = activeProfileListeners.size() - 1; i >= 0; i--) {
								activeProfileListeners.get(i).activeProfileChanged(state.device, newProfile);
							}
						}
					}
				}
			}
		});
	}

	protected void checkDevice(MacroDevice device) {
		if (!devices.containsKey(device))
			throw new IllegalArgumentException("The device must be added to the macro system first.");
	}

	protected MacroProfile doCreateProfile(MacroDevice device, String name) {
		synchronized (devices) {
			MacroProfile profile = new MacroProfile(this, device, UUID.randomUUID());
			profile.setName(name);
			profile.addBank("Default");
			try {
				storage.saveProfile(profile);
			} catch (IOException ioe) {
				throw new IllegalStateException("Failed to create profile.", ioe);
			}
			return profile;
		}
	}

	protected void fireMacroSystemChanged() {
		for (int i = macroSystemListeners.size() - 1; i >= 0; i--)
			macroSystemListeners.get(i).macroSystemChanged();
	}

	protected MacroProfile loadActiveProfile(MacroDevice device) throws IOException {
		UUID id = storage.loadActiveProfile(device);
		MacroProfile profile = id == null ? null : storage.loadProfile(device, id);
		if (profile == null) {
			UUID defId = storage.loadDefaultProfile(device);
			if (Objects.equals(defId, id)) {
				profile = storage.loadProfile(device, defId);
			}
			if (profile == null) {
				profile = createDefault(device);
				storage.saveProfile(profile);
				storage.setActiveProfile(profile);
				fireMacroSystemChanged();
			}
		}

		return profile;
	}

	protected void pushActiveProfile(MacroProfile macroProfile) throws IOException {
		if (!macroProfile.equals(getActiveProfile(macroProfile.getDevice()))) {
			LOG.log(Level.INFO, String.format("Pushing profile %s (%s)", macroProfile.getId(), macroProfile.getName()));

			MacroDevice device = macroProfile.getDevice();
			checkDevice(device);
			if (isLocked(macroProfile.getDevice()))
				throw new IllegalStateException("Profile is locked on this device.");
			List<MacroProfile> profiles = devices.get(device).activeProfiles;
			while (profiles.size() > 1)
				profiles.remove(0);
			profiles.add(0, macroProfile);
			for (int i = activeProfileListeners.size() - 1; i >= 0; i--)
				activeProfileListeners.get(i).activeProfileChanged(device, macroProfile);
		}
	}

	void bankAdded(MacroBank bank) {
		try {
			storage.saveProfile(bank.getProfile());
			fireProfileChange(bank.getProfile());
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to update profile.", ioe);
		}
	}

	/**
	 * 
	 * Create the default activeProfiles for the specified device if it doesn't
	 * exist.
	 * 
	 * @param device device associated with default activeProfiles
	 * @return default activeProfiles for device
	 * @throws IOException on error
	 */
	MacroProfile createDefault(MacroDevice device) throws IOException {
		MacroProfile defaultProfile = new MacroProfile(this, device, UUID.randomUUID());
		defaultProfile.setName("Default");
		defaultProfile.addBank("Default");
		return defaultProfile;
	}

	void fireProfileChange(MacroProfile profile) {
		for (int i = profileListeners.size() - 1; i >= 0; i--)
			profileListeners.get(i).profileChanged(profile.getDevice(), profile);
	}

	void fireProfileChanged(MacroDevice device, MacroProfile profile) {
		for (int i = profileListeners.size() - 1; i >= 0; i--) {
			profileListeners.get(i).profileChanged(device, profile);
		}

	}

	void profileChanged(MacroProfile profile) {
		try {
			storage.saveProfile(profile);
			fireProfileChange(profile);
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to update profile.", ioe);
		}
	}

	void removedBank(MacroBank macroBank) {
		MacroProfile profile = macroBank.getProfile();
		try {
			storage.saveProfile(profile);
			fireProfileChange(profile);
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to remove macro bank.", ioe);
		}
	}

	void removedProfile(MacroProfile profile) {
		try {
			storage.removeProfile(profile);
			fireMacroSystemChanged();
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to remove macro bank.", ioe);
		}
	}

	private boolean processForRecording(EventCode key, KeyState state, Event event, MacroDevice device) {
		switch (recordingSession.getRecordingState()) {
		default:
			/* Pass on event as normal */
			return true;
		}
	}

}
