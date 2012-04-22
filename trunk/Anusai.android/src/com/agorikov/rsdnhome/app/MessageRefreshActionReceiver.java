package com.agorikov.rsdnhome.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MessageRefreshActionReceiver extends BroadcastReceiver {

	public static final String ACTION_REFRESH_MESSAGES = "com.agorikov.rsdnhome.ACTION_REFRESH_MESSAGES";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final Intent startIntent = new Intent(context, AnusaiService.class);
		context.startService(startIntent);
	}

}
