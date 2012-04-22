package com.agorikov.rsdnhome.beans;

public interface ChangeListener<T> {
	void onChange(Observable bean, T oldValue, T newValue);
}
