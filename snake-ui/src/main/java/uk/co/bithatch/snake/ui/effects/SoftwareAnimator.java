package uk.co.bithatch.snake.ui.effects;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

/**
 * Drives a software effect by sending a static colour to the device on a timer.
 * Used for devices that only support static colour in their driver (e.g. the
 * Razer Naga Trinity under OpenRazer).
 */
class SoftwareAnimator {

	final static System.Logger LOG = System.getLogger(SoftwareAnimator.class.getName());

	/** Frame period. ~20 fps is smooth enough and light on USB. */
	static final long FRAME_MS = 50;

	interface Frame {
		/**
		 * @param elapsed milliseconds since the effect started
		 * @return colour to show now, r/g/b 0-255
		 */
		int[] colorAt(long elapsed);
	}

	private ScheduledFuture<?> task;
	private int[] last;
	private int failures;

	/**
	 * Software effects run on whole devices only (static colour is device-wide).
	 */
	static boolean isSupported(Lit component, Class<?> hardwareEffect) {
		return component instanceof Device && !component.getSupportedEffects().contains(hardwareEffect)
				&& component.getSupportedEffects().contains(Static.class);
	}

	synchronized void start(App context, Lit component, Frame frame) {
		stop();
		last = null;
		failures = 0;
		long started = System.currentTimeMillis();
		task = context.getSchedulerManager().get(Queue.DEVICE_IO).scheduleAtFixedRate(() -> {
			try {
				int[] rgb = frame.colorAt(System.currentTimeMillis() - started);
				if (Arrays.equals(rgb, last))
					return;
				last = rgb;
				Static s = component.createEffect(Static.class);
				s.setColor(rgb);
				/* updateEffect doesn't fire change events for every frame */
				component.updateEffect(s);
				failures = 0;
			} catch (Exception e) {
				/* Device probably went away; stop rather than spin */
				if (++failures >= 20) {
					LOG.log(Level.WARNING, "Stopping software effect after repeated errors.", e);
					stop();
				}
			}
		}, 0, FRAME_MS, TimeUnit.MILLISECONDS);
	}

	synchronized void stop() {
		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}

	static int[] scale(int[] rgb, float level) {
		return new int[] { Math.round(rgb[0] * level), Math.round(rgb[1] * level), Math.round(rgb[2] * level) };
	}
}
