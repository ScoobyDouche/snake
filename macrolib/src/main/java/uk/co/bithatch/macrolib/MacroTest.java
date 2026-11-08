package uk.co.bithatch.macrolib;

import uk.co.bithatch.linuxio.InputDevice;
import uk.co.bithatch.linuxio.InputDevice.Event;


/**
 * The Class MacroTest.
 */
public class MacroTest {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception {
		try(InputDevice mouse = new InputDevice("/dev/input/event19")) {
	        mouse.grab();
	        while (true) {
				Event ev = mouse.nextEvent();
				if (ev == null) {
					break;
				}
				System.out.println(ev);
			}
		}
	}
}
