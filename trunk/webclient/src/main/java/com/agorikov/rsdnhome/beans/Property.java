package com.agorikov.rsdnhome.beans;

public class Property<T> extends ObservableValue<T> {

	private T value;
	
	public Property(final T value) {
		this.value = value;
	}
	
	@Override
	public final T get() {
		return value;
	}
	
	public final void set(T newValue) {
		if (differs(value, newValue)) {
			T oldValue = value;
			value = newValue;
			if (hasInvalidationListeners())
				triggerInvalidateNotification();
			if (hasChangeListeners())
				triggerChangeNotification(oldValue, newValue);
		}
	}
	
	protected boolean differs(T a, T b) {
		if (a == null)
			return b != null;
		return !a.equals(b);
	}
	
}
