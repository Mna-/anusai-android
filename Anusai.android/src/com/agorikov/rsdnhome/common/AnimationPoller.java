package com.agorikov.rsdnhome.common;

import java.util.LinkedList;
import java.util.WeakHashMap;

import android.os.Handler;

/**
 * Here will be code for polling UIs
 * @author artem
 *
 */
public final class AnimationPoller {
	// polling interval 100ms
	static final int PollingInterval = 99;
	private final WeakHashMap<Runnable, Void> subscribers = new WeakHashMap<Runnable, Void>();
	private boolean active;
	
	private static final Handler handler = new Handler();
	
	private final Runnable callback = new Runnable() {
		@Override
		public void run() {
			boolean subscribed = false;
			for (final Runnable subscriber : new LinkedList<Runnable>(subscribers.keySet())) {
				subscribed = true;
				subscriber.run();
			}
			if (active = subscribed) {
				handler.postDelayed(this, PollingInterval);
			}
		}
	};
	
	private static final AnimationPoller instance_ = new AnimationPoller();
	
	public static AnimationPoller getInstance() {
		return instance_;
	}
	
	public void subscribe(final Runnable subscriber) {
		subscribers.put(subscriber, null);
		if (!active) {
			callback.run();
		}
	}
	
	public void unsubscribe(final Runnable subscriber) {
		subscribers.remove(subscriber);
	}
	
	
}
