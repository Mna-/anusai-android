package com.agorikov.rsdnhome.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.HandlerCompositeUtils;

public class CredentialsActivity extends Activity {
    public static final String TAG = "CredentialsActivity";
	public static final String CREDENTIALS_MODIFIED = "CredentialsModified";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.credentials);
    	
    	final EditText edUserName = (EditText) findViewById(R.id.edUserName);
    	final EditText edPassword = (EditText) findViewById(R.id.edPassword);
    	
		final SharedPreferences pref = getApplicationContext().getSharedPreferences(
				"credentials", Activity.MODE_PRIVATE);
		edUserName.setText(pref.getString("userName", ""));

		final Button saveBtn = (Button) findViewById(R.id.saveBtn);
		saveBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Editor editor = pref.edit();
				editor.putString("userName", edUserName.getText().toString()); 
				editor.putString("password", edPassword.getText().toString()); 
				editor.commit();
				sendBroadcast(new Intent(CREDENTIALS_MODIFIED));
				finish();
			}
		});
		
		final Property<Boolean> badCredentialsProperty = RSDNApplication.getInstance().badCredentialsProperty();
		badCredentialsProperty.addChangeListener(badCredentialsListener);
		badCredentialsListener.onChange(badCredentialsProperty, null, badCredentialsProperty.get());
    }
    
    private final ChangeListener<Boolean> badCredentialsListener = HandlerCompositeUtils.wrapPostponedListener(new ChangeListener<Boolean>() {
		@Override
		public void onChange(Observable bean, Boolean oldValue, Boolean newValue) {
			final TextView authenticationFailureText = (TextView) findViewById(
					R.id.authenticationFailureText);
			authenticationFailureText.setVisibility(newValue ? View.VISIBLE : View.GONE);
		}});
    
}
