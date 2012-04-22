package com.agorikov.rsdnhome.app;

import com.agorikov.rsdnhome.app.R;
import com.agorikov.rsdnhome.model.RowVersion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.agorikov.rsdnhome.common.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class anusaiActivity extends Activity {

    public static final String TAG = "anusaiActivity";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		Log.d(TAG, "Hello world");
        
        final Button bnForums = (Button) findViewById(R.id.bnForums);
        bnForums.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "bnForums clicked");
				launchForumList();
			}
		});
        final Button bnMessages = (Button) findViewById(R.id.bnMessages);
        bnMessages.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "bnMessages clicked");
				launchMessageList();
			}
		});
        final Button bnCleanUsersRowVersion = (Button) findViewById(R.id.bnCleanUsersRowVersion);
        bnCleanUsersRowVersion.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "bnCleanUsersRowVersion clicked");
				cleanRowVersion(RowVersion.Users);
			}
		});
        final Button bnCleanMessagesRowVersion = (Button) findViewById(R.id.bnCleanMessagesRowVersion);
        bnCleanMessagesRowVersion.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "bnCleanMessagesRowVersion clicked");
				cleanRowVersion(RowVersion.Message);
			}
		});
        bnCleanUsersRowVersion.setEnabled(false);
        bnCleanMessagesRowVersion.setEnabled(false);
        
    }

	protected void cleanRowVersion(final RowVersion rowVersion) {
		rowVersion.put(RowVersion.INITIAL_BASE64_VERSION);
		final Toast toast = Toast.makeText(getApplicationContext(), String.format("Row version for %s reset", rowVersion), Toast.LENGTH_LONG);
		toast.show();
	}

	protected void launchForumList() {
		final Intent i = new Intent(this, ForumsActivity.class);
		startActivity(i);
	}

	protected void launchMessageList() {
		final Intent i = new Intent(this, MessagesActivity.class);
		startActivity(i);
	}
}