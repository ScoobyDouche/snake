package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class ReactiveEffectHandler extends AbstractBackendEffectHandler<Reactive> {

	public ReactiveEffectHandler() {
		super(Reactive.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Reactive effect) {
		effect.setSpeed(prefs.getInt("speed", 100));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
	}

	@Override
	protected void onSave(Preferences prefs, Reactive effect) {
		prefs.putInt("speed", effect.getSpeed());
		prefs.put("color", Colors.toHex(effect.getColor()));
	}


}
