package com.agorikov.rsdnhome.beans;

import java.util.WeakHashMap;

public abstract class Observable {

	private final WeakHashMap<InvalidationListener, Void> listeners = new WeakHashMap<InvalidationListener, Void>();
	
	public final void addInvalidationListener(InvalidationListener listener) {
		listeners.put(listener, null);
	}
	
	public final void removeInvalidationListener(InvalidationListener listener) {
		listeners.remove(listener);
	}
	
	protected final void triggerInvalidateNotification() {
		for (final InvalidationListener listener : listeners.keySet()) {
			listener.onInvalidated();
		}
	}
	
	protected final boolean hasInvalidationListeners() {
		return !listeners.isEmpty();
	}
	
}
