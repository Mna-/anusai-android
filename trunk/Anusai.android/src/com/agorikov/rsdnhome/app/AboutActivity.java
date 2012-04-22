package com.agorikov.rsdnhome.app;

import com.agorikov.rsdnhome.common.util.Log;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AboutActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);
		super.onCreate(savedInstanceState);
		
		updateLogChk();
		
		final SharedPreferences pref = getApplicationContext().getSharedPreferences("LogMode", Activity.MODE_PRIVATE);
		final CheckBox enableLogChk = (CheckBox) findViewById(R.id.enableLogChk);
		enableLogChk.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				final Editor editor = pref.edit();
				editor.putBoolean("enableLog", isChecked);
				editor.commit();
				Log.enableFileLog(isChecked);
			}});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateLogChk();
	}

	private void updateLogChk() {
		final SharedPreferences pref = getApplicationContext().getSharedPreferences("LogMode", Activity.MODE_PRIVATE);
		final CheckBox enableLogChk = (CheckBox) findViewById(R.id.enableLogChk);
		enableLogChk.setChecked(pref.getBoolean("enableLog", false));
	}
	
}
