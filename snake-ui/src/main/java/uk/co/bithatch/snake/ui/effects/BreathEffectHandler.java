package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Breath.Mode;

public class BreathEffectHandler extends AbstractBackendEffectHandler<Breath> {

	public BreathEffectHandler() {
		super(Breath.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Breath effect) {
		effect.setMode(Mode.valueOf(prefs.get("mode", Mode.RANDOM.name())));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
		effect.setColor1(Colors.fromHex(prefs.get("color1", "#00ff00")));
		effect.setColor2(Colors.fromHex(prefs.get("color2", "#0000ff")));
	}

	@Override
	protected void onSave(Preferences prefs, Breath effect) {
		prefs.put("mode", effect.getMode().name());
		prefs.put("color", Colors.toHex(effect.getColor()));
		prefs.put("color1", Colors.toHex(effect.getColor1()));
		prefs.put("color2", Colors.toHex(effect.getColor2()));
	}


}
