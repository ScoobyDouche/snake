package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public abstract class AbstractBackendEffectHandler<E extends Effect> extends AbstractPersistentEffectHandler<E> {

	protected AbstractBackendEffectHandler(Class<E> clazz) {
		super(clazz);
	}

	public Class<E> getBackendEffectClass() {
		return getEffectClass();
	}

	@Override
	protected final E onActivate(Lit component) {
		E effect = (E) component.createEffect(getEffectClass());

		/* Load the configuration for this effect */
		onLoad(getEffectPreferences(component), effect);

		apply(component, effect);

		return effect;
	}

	/**
	 * The effect's current settings: the running effect if it is this one, else
	 * the saved settings. Returns a copy, change it and pass it to
	 * {@link #storeEffect(Lit, Effect)}.
	 */
	@SuppressWarnings("unchecked")
	public E loadEffect(Lit component) {
		Effect d = component.getEffect();
		if (d != null && d.getClass().equals(getEffectClass())) {
			try {
				return (E) d.clone();
			} catch (CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
		}
		E e = (E) component.createEffect(getEffectClass());
		onLoad(getEffectPreferences(component), e);
		return e;
	}

	/** Apply and remember new settings for this effect. */
	public void storeEffect(Lit component, E effect) {
		apply(component, effect);
		save(component, effect);
	}

	/**
	 * Send the effect to the device. Software effects override this to animate it
	 * instead.
	 */
	protected void apply(Lit component, E effect) {
		getContext().getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> component.setEffect(effect));
	}

}
