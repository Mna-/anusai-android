package com.agorikov.rsdnhome.beans;

import java.util.WeakHashMap;

public abstract class ObservableValue<T> extends Observable {
	
	private final WeakHashMap<ChangeListener<T>, Void> listeners = new WeakHashMap<ChangeListener<T>, Void>();

	public abstract T get();
	
	public final void addChangeListener(ChangeListener<T> listener) {
		listeners.put(listener, null);
	}
	
	public final void removeChangeListener(ChangeListener<T> listener) {
		listeners.remove(listener);
	}
	
	protected final void triggerChangeNotification(final T oldValue, final T newValue) {
		for (final ChangeListener<T> listener : listeners.keySet()) {
			listener.onChange(this, oldValue, newValue);
		}
	}
	
	protected final boolean hasChangeListeners() {
		return !listeners.isEmpty();
	}
}
