package uk.co.bithatch.snake.ui.effects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lets other code pause Snake's software lighting effects. They send a frame
 * to the device many times a second, which drowns out the replies to direct
 * HID commands (e.g. onboard profiles).
 */
public final class SoftwareEffects {

	private static final AtomicInteger PAUSED = new AtomicInteger();

	private SoftwareEffects() {
	}

	public static void pause() {
		PAUSED.incrementAndGet();
	}

	public static void resume() {
		PAUSED.updateAndGet(i -> Math.max(0, i - 1));
	}

	public static boolean isPaused() {
		return PAUSED.get() > 0;
	}
}
