package com.agorikov.rsdnhome.common;

import android.os.Handler;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;

public class HandlerCompositeUtils {

	
	public static Runnable wrapPostponedRunnable(final Runnable r) {
		return new Runnable() {
			final Handler handler = new Handler();
			volatile boolean pending;
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					pending = false;
					r.run();
				}};
			@Override
			public void run() {
				if (!pending) {
					pending = true;
					handler.postDelayed(runnable, 100);
				}
			}
		};
	}
	
	
	public static <T> ChangeListener<T> wrapPostponedListener(final ChangeListener<T> listener) {
		return new ChangeListener<T>() {
			final Handler handler = new Handler();
			volatile boolean pending;
			volatile Observable bean;
			volatile T oldValue;
			volatile T newValue;
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					pending = false;
					listener.onChange(bean, oldValue, newValue);
				}};
			
			@Override
			public void onChange(Observable bean, T oldValue, final T newValue) {
				this.newValue = newValue;
				if (!pending) {
					pending = true;
					this.bean = bean;
					this.oldValue = oldValue;
					handler.postDelayed(runnable, 100);
				}
			}
			
		};
	}
	
}
