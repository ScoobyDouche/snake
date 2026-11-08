package uk.co.bithatch.snake.ui.effects;

import java.util.Arrays;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.effects.Static;

/**
 * Audio effect for devices without a key matrix whose driver only supports
 * static colour (e.g. Razer Naga Trinity).
 * <p>
 * The normal audio modes draw onto a small virtual matrix (so the options
 * screen and its preview work as usual); each frame is then folded into one
 * colour: the average colour of the lit cells, brighter the more cells are lit.
 */
public class SoftwareAudioEffectHandler extends AudioEffectHandler {

	/** Virtual canvas, [rows, columns]. */
	private static final int[] VIRTUAL_SIZE = { 6, 12 };

	private int[] last;

	@Override
	public boolean isSupported(Lit component) {
		return SoftwareAnimator.isSupported(component, Matrix.class);
	}

	@Override
	protected int[] getMatrixSize(Lit component) {
		return VIRTUAL_SIZE;
	}

	@Override
	public void update(Lit component) {
		int[][][] frame = getFrame();
		if (frame == null)
			return;
		long r = 0, g = 0, b = 0;
		int lit = 0, cells = 0;
		for (int[][] row : frame) {
			for (int[] px : row) {
				cells++;
				if (px[0] + px[1] + px[2] > 0) {
					lit++;
					r += px[0];
					g += px[1];
					b += px[2];
				}
			}
		}
		int[] rgb;
		if (lit == 0) {
			rgb = new int[3];
		} else {
			/* A third of the canvas lit is already full brightness */
			float level = Math.min(1f, (lit / (float) cells) * 3f);
			rgb = SoftwareAnimator.scale(new int[] { (int) (r / lit), (int) (g / lit), (int) (b / lit) }, level);
		}
		if (Arrays.equals(rgb, last))
			return;
		last = rgb;
		Device device = Lit.getDevice(component);
		Static s = device.createEffect(Static.class);
		s.setColor(rgb);
		/* updateEffect doesn't fire change events for every frame */
		device.updateEffect(s);
	}

	@Override
	public void deactivate(Lit component) {
		super.deactivate(component);
		last = null;
	}
}
