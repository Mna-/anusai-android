package com.agorikov.rsdnhome.app;

import com.agorikov.rsdnhome.beans.Property;

import android.app.Activity;

public abstract class BaseAnusaiActivity extends Activity {

	private final Property<Integer> activeViewCount = 
			RSDNApplication.getInstance().activeViewCountProperty();
	
	@Override
	protected void onPause() {
		super.onPause();
		activeViewCount.set(activeViewCount.get() - 1);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		activeViewCount.set(activeViewCount.get() + 1);
	}
	
}
