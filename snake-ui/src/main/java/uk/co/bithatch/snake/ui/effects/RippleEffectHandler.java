package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Ripple.Mode;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class RippleEffectHandler extends AbstractBackendEffectHandler<Ripple> {

	public RippleEffectHandler() {
		super(Ripple.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Ripple effect) {
		effect.setMode(Mode.valueOf(prefs.get("mode", Mode.RANDOM.name())));
		effect.setRefreshRate(prefs.getDouble("refreshRate", 100));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
	}

	@Override
	protected void onSave(Preferences prefs, Ripple effect) {
		prefs.put("mode", effect.getMode().name());
		prefs.putDouble("refreshRate", effect.getRefreshRate());
		prefs.put("color", Colors.toHex(effect.getColor()));
	}


}
