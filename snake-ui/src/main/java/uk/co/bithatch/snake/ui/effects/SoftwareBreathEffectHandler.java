package uk.co.bithatch.snake.ui.effects;

import java.util.Random;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Breath;

/**
 * Breath (fade in and out) for devices whose driver only supports static
 * colour. Supports the same modes as hardware breath: one colour, alternating
 * two colours, or a random colour each breath.
 */
public class SoftwareBreathEffectHandler extends BreathEffectHandler {

	/** One full breath, in and out. */
	static final long BREATH_MS = 4000;

	private final SoftwareAnimator animator = new SoftwareAnimator();
	private final Random random = new Random();

	@Override
	public boolean isSupported(Lit component) {
		return SoftwareAnimator.isSupported(component, Breath.class);
	}

	@Override
	protected void apply(Lit component, Breath effect) {
		Breath.Mode mode = effect.getMode() == null ? Breath.Mode.RANDOM : effect.getMode();
		int[] color = effect.getColor();
		int[] color1 = effect.getColor1();
		int[] color2 = effect.getColor2();
		int[][] randomColor = new int[1][];
		long[] randomCycle = { -1 };

		animator.start(getContext(), component, (elapsed) -> {
			long cycle = elapsed / BREATH_MS;
			double phase = (elapsed % BREATH_MS) / (double) BREATH_MS;
			/* 0 -> 1 -> 0, eased so it lingers at the ends like a real breath */
			float level = (float) ((1 - Math.cos(phase * 2 * Math.PI)) / 2);
			int[] base;
			switch (mode) {
			case SINGLE:
				base = color;
				break;
			case DUAL:
				base = cycle % 2 == 0 ? color1 : color2;
				break;
			default:
				if (randomCycle[0] != cycle) {
					randomCycle[0] = cycle;
					randomColor[0] = Colors.toRGB(random.nextFloat(), 1f, 1f);
				}
				base = randomColor[0];
				break;
			}
			return SoftwareAnimator.scale(base, level);
		});
	}

	@Override
	public boolean isRegions() {
		/* Static colour is device-wide, so these run on the whole device */
		return false;
	}

	@Override
	public void deactivate(Lit component) {
		animator.stop();
	}

	@Override
	public void removed(Device device) {
		animator.stop();
	}
}
