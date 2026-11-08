package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Spectrum;

/**
 * Spectrum (rainbow cycle) for devices whose driver only supports static
 * colour. Snake cycles the colour itself while it is running.
 */
public class SoftwareSpectrumEffectHandler extends SpectrumEffectHandler {

	/** One full trip round the colour wheel. */
	static final long CYCLE_MS = 8000;

	private final SoftwareAnimator animator = new SoftwareAnimator();

	@Override
	public boolean isSupported(Lit component) {
		return SoftwareAnimator.isSupported(component, Spectrum.class);
	}

	@Override
	protected void apply(Lit component, Spectrum effect) {
		animator.start(getContext(), component,
				(elapsed) -> Colors.toRGB((elapsed % CYCLE_MS) / (float) CYCLE_MS, 1f, 1f));
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
