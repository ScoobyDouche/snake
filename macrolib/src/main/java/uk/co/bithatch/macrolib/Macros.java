package uk.co.bithatch.macrolib;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import uk.co.bithatch.linuxio.EventCode;

/**
 * The Class Macros.
 */
public class Macros {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public final static void main(String[] args) throws Exception {
		Path testdir = Paths.get("/tmp/macrotest");
		
		MacroSystem msys = new MacroSystem(new JsonMacroStorage(testdir));
		MacroDevice dev = new AbstractUInputMacroDevice("/dev/input/event6") {
			private Map<String, ActionBinding> actions = new HashMap<>();

			{
//				addAction(new ActionBinding(this, MacroSystem.ACTION_CYCLE_BANK, EventCode.KEY_TAB));
			}

			@Override
			public Map<String, ActionBinding> getActionKeys() {
				return actions;
			}

			@Override
			public int getBanks() {
				return 3;
			}

			@Override
			public int getJoystickCalibration() {
				return 0;
			}

			@Override
			public TargetType getJoystickMode() {
				return TargetType.DIGITAL_JOYSTICK;
			}
		};
		msys.addDevice(dev);

		MacroProfile profile = msys.getDefaultProfile(dev);
		MacroBank bank = profile.getBank(0);
		
		/* WORK OK */
		bank.add(new UInputMacro(new KeySequence(EventCode.KEY_1), EventCode.KEY_2));
		bank.add(new UInputMacro(new KeySequence(KeyState.UP, EventCode.KEY_2), EventCode.KEY_4).repeatMode(RepeatMode.NONE));
		bank.add(new CommandMacro(new KeySequence(KeyState.UP, EventCode.KEY_3), "xdg-open", "http://www.bithatch.co.uk/"));
		bank.add(new SimpleMacro(new KeySequence(EventCode.KEY_4), "HelloWorld! 123456789 !\"£$%^&*()"));
		bank.add(new ScriptMacro(new KeySequence(EventCode.KEY_5), "upress keyboard KEY_1", "delay 1000",
				"urelease keyboard KEY_1"));
		bank.add(new UInputMacro(new KeySequence(KeyState.HELD, EventCode.KEY_TAB), EventCode.KEY_A));
		bank.add(new UInputMacro(new KeySequence(KeyState.HELD, EventCode.KEY_Q), EventCode.KEY_B).repeatMode(RepeatMode.NONE));
		bank.add(new UInputMacro(new KeySequence(EventCode.KEY_W), EventCode.KEY_X).repeatMode(RepeatMode.TOGGLE));
		bank.add(new UInputMacro(new KeySequence(KeyState.UP, EventCode.KEY_E), EventCode.KEY_X).repeatMode(RepeatMode.TOGGLE));
		
		/* Need work - activating too early and repeating */
		bank.add(new UInputMacro(new KeySequence(KeyState.HELD, EventCode.KEY_R), EventCode.KEY_Y).repeatMode(RepeatMode.TOGGLE));
		bank.add(new UInputMacro(new KeySequence(EventCode.KEY_CAPSLOCK, EventCode.KEY_A), EventCode.KEY_V));
		
//		bank.add(new ActionMacro(new KeySequence(EventCode.KEY_LEFTALT), MacroSystem.ACTION_CYCLE_BANK));

//		MacroProfile profile2 = msys.getOrCreateProfileWithName(dev, "MyProfile");
//		profile2.getIncludeApplications().add(".*Eclipse.*");
//		MacroBank bank2 = profile2.getBank(0);
//		bank2.add(new UInputMacro(new KeySequence(EventCode.KEY_1), EventCode.KEY_9));
//		MacroBank bank3 = profile2.getBank(1);
//		bank3.add(new UInputMacro(new KeySequence(EventCode.KEY_1), EventCode.KEY_8));

		msys.open();
	}
}
