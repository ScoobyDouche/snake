package uk.co.bithatch.snake.ui.effects;

import java.net.URL;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Lit;

public abstract class AbstractPersistentEffectHandler<E> extends AbstractEffectHandler<E> {

	private Class<E> clazz;

	protected AbstractPersistentEffectHandler(Class<E> clazz) {
		this.clazz = clazz;
	}

	@Override
	public URL getEffectImage(int size) {
		return getContext().getConfiguration().getTheme().getEffectImage(size, clazz);
	}
	
	protected Class<E> getEffectClass() {
		return clazz;
	}


	@Override
	public boolean isSupported(Lit component) {
		return component.getSupportedEffects().contains(clazz);

	}

	@Override
	public String getDisplayName() {
		return bundle.getString("effect." + clazz.getSimpleName());
	}

	public void save(Lit component, E effect) {
		onSave(getEffectPreferences(component), effect);
	}

	public void load(Lit component, E effect) {
		onLoad(getEffectPreferences(component), effect);
	}

	protected void onLoad(Preferences prefs, E effect) {
	}

	protected void onSave(Preferences prefs, E effect) {
	}
}
