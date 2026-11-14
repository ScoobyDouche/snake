package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Wave;
import uk.co.bithatch.snake.lib.effects.Wave.Direction;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class WaveEffectHandler extends AbstractBackendEffectHandler<Wave> {

	public WaveEffectHandler() {
		super(Wave.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Wave effect) {
		effect.setDirection(Direction.valueOf(prefs.get("direction", Direction.FORWARD.name())));
	}

	@Override
	protected void onSave(Preferences prefs, Wave effect) {
		prefs.put("direction", effect.getDirection().name());
	}


}
