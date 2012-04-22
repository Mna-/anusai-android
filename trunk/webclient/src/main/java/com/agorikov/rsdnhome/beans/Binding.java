package com.agorikov.rsdnhome.beans;

import java.util.WeakHashMap;

public abstract class Binding<T> extends ObservableValue<T> {
	private T value;
	private boolean valid;
	private final InvalidationListener listener = new InvalidationListener() {
		@Override
		public void onInvalidated() {
			invalidate();
		}
	};
	private final WeakHashMap<Observable, Void> boundTo = new WeakHashMap<Observable, Void>();
	
	
	
	public T get() {
		if (!valid) {
			valid = true;
			value = calculate();
		}
		return value;
	}

	protected final void bind(final Observable ...beans) {
		for (final Observable bean : beans) {
			bean.addInvalidationListener(listener);
			boundTo.put(bean, null);
		}
	}
	
	protected final void unbind() {
		for (final Observable bean : boundTo.keySet()) {
			bean.removeInvalidationListener(listener);
		}
		boundTo.clear();
	}
	
	public final void invalidate() {
		final T oldValue = value;
		valid = false;
		if (hasInvalidationListeners())
			triggerInvalidateNotification();
		if (hasChangeListeners())
			triggerChangeNotification(oldValue, get());
	}
	
	protected abstract T calculate();
}
