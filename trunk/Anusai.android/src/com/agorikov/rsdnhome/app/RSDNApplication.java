package com.agorikov.rsdnhome.app;

import java.sql.SQLException;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.ComposedMessages;
import com.agorikov.rsdnhome.model.Credentials.CredentialsBuilder;
import com.agorikov.rsdnhome.model.ForumGroups;
import com.agorikov.rsdnhome.model.Forums;
import com.agorikov.rsdnhome.model.Messages;
import com.agorikov.rsdnhome.model.Preferences;
import com.agorikov.rsdnhome.model.Topics;
import com.agorikov.rsdnhome.model.Users;
import com.agorikov.rsdnhome.persist.DataModel;
import com.agorikov.rsdnhome.webclient.model.RsdnWebService;

public final class RSDNApplication extends Application {
    public static final String TAG = "RSDNApplication";
    public static final int CACHED_ENTITIES_SIZE = 1; //1000;
    private static final Handler handler = new Handler();

	private static RSDNApplication singleton;
	private final RsdnWebService ws;
	private final DataModel dataModel;
	private final Forums forums;
	private final ForumGroups forumGroups;
	private final Messages messages;
	private final Users users;
	private final Preferences preferences;
	private final Topics topics;
	private final ComposedMessages composedMessages;

	private void registerULog() {
		final Log.UtilLog uLog = new Log.UtilLog() {
			@Override
			public void d(String tag, String msg) {
				android.util.Log.d(tag, msg);
			}
			@Override
			public void d(String tag, String msg, Throwable e) {
				android.util.Log.d(tag, msg, e);
			}

			@Override
			public void e(String tag, String msg) {
				android.util.Log.e(tag, msg);
			}

			@Override
			public void e(String tag, String msg, Throwable e) {
				android.util.Log.e(tag, msg, e);
			}};
		Log.registerUtilLog(uLog);
		final SharedPreferences pref = getApplicationContext().getSharedPreferences("LogMode", Activity.MODE_PRIVATE);
		Log.enableFileLog(pref.getBoolean("enableLog", false));
	}
	
	public RSDNApplication() {
		this.dataModel = new DataModel();
		this.ws = new RsdnWebService("Anusai.android-0.0.1", dataModel.connection());

		try {
			forums = new Forums(dataModel.connection());
			forumGroups = new ForumGroups(dataModel.connection());
			messages = new Messages(dataModel.connection());
			users = new Users(dataModel.connection());
			preferences = new Preferences(dataModel.connection());
			topics = new Topics(dataModel.connection());
			composedMessages = new ComposedMessages(dataModel.connection());
		} catch (SQLException e) {
			throw new RuntimeException("Error in DB access in application initialization", e);
		}
		
	}
	
	private void readCredentials() {
		final SharedPreferences pref = getApplicationContext().getSharedPreferences("credentials",  Activity.MODE_PRIVATE);

		final CredentialsBuilder credentialsBuilder = CredentialsBuilder.create();
		credentialsBuilder.userName(pref.getString("userName", ""));
		credentialsBuilder.password(pref.getString("password", ""));
		
		ws.setCredentials(credentialsBuilder.build());
	}

	final BroadcastReceiver credentialsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			readCredentials();
		}
	};
	
	
	
	public static RSDNApplication getInstance() {
		return singleton;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				registerULog();
				readCredentials();
				registerReceiver(credentialsReceiver, new IntentFilter(CredentialsActivity.CREDENTIALS_MODIFIED));
				AnusaiService.updateAlarms();
			}});
	}
	
	public RsdnWebService getWebService() {
		return ws;
	}
	
	public DataModel getDataModel() {
		return dataModel;
	}
	
	public Forums getForums() {
		return forums;
	}
	public ForumGroups getForumGroups() {
		return forumGroups;
	}
	public Messages getMessages() {
		return messages;
	}
	public Users getUsers() {
		return users;
	}
	public Preferences getPreferences() {
		return preferences;
	}

	public Topics getTopics() {
		return topics;
	}
	
	public ComposedMessages getComposedMessages() {
		return composedMessages;
	}
	
	public void flushData() {
		dataModel.flush();
	}

}
