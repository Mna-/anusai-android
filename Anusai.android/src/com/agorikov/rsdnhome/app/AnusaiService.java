package com.agorikov.rsdnhome.app;

import java.util.LinkedList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.HandlerCompositeUtils;
import com.agorikov.rsdnhome.common.MessageViewFormatter;
import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.MessageEntity;
import com.agorikov.rsdnhome.model.User;
import com.agorikov.rsdnhome.model.UserEntity;
import com.agorikov.rsdnhome.webclient.model.RsdnWebService;
import com.agorikov.rsdnhome.webclient.model.SimpleWebMethod.EntityReceiver;

public class AnusaiService extends Service {
	static final String TAG = "AnusaiService";

	static final int NOTIFICATION_FOREGROUND_ID = 1;
	public static final String NEW_DATA_RECEIVED = "New_Data_Received";
	static final Handler handler = new Handler();
	private AsyncTask<Void,Void,Void> asyncTask;
	
	private NotificationManager notifications;
	
	private final Property<Integer> newMessageCount = new Property<Integer>(0);
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notifications = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		moveForeground();
	}

	private ChangeListener<Integer> newMessageCountListener;
	private Message lastReceivedMessage;
	
	private void moveForeground() {
		final Intent intent = new Intent(this, MessagesActivity.class);
		final PendingIntent pi = PendingIntent.getActivity(this, NOTIFICATION_FOREGROUND_ID, intent, 0);
		final Context ctx = getApplicationContext();
		final Notification notificationForeground = new Notification(R.drawable.stat_notify_sync, 
				ctx.getText(R.string.dataUpdate), System.currentTimeMillis());
		final CharSequence notificationTitle = ctx.getText(R.string.app_name);
		notificationForeground.setLatestEventInfo(this, notificationTitle, ctx.getText(R.string.dataUpdate), pi);
		notificationForeground.flags |= Notification.FLAG_ONGOING_EVENT;

		final CharSequence newMsgNotificationText = ctx.getText(R.string.notifyNewMessage);
		final Notification notificationNewMessages = new Notification(R.drawable.rsdn, 
				newMsgNotificationText, System.currentTimeMillis());
		notificationNewMessages.setLatestEventInfo(this, notificationTitle, newMsgNotificationText, pi);
		notificationNewMessages.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS
				| Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND;
		
		final int notifId = (int) 30984937;//System.currentTimeMillis();
		
		newMessageCountListener = HandlerCompositeUtils.wrapPostponedListener(new ChangeListener<Integer>() {
			int lastReceivedBroadcastedCount = 0;
			@Override
			public void onChange(Observable bean, Integer oldValue,
				final Integer newValue) {
				if (newValue - lastReceivedBroadcastedCount >= 1000) {
					broadcastNewDataNotification();
					lastReceivedBroadcastedCount = newValue;
				}
				
				notificationNewMessages.number = newValue;
				notificationNewMessages.when = System.currentTimeMillis();
				
				if (notificationNewMessages.number == 1) {
					final long msgId = lastReceivedMessage.getId();
					final Intent intent = new Intent(AnusaiService.this, MessageViewActivity.class);
					intent.putExtra("messageId", msgId);
					notificationNewMessages.contentIntent = PendingIntent.getActivity(ctx, notifId, intent, 0);
					notificationNewMessages.setLatestEventInfo(ctx, notificationTitle, 
							 MessageViewFormatter.format(lastReceivedMessage.getSubject(), null),
							 notificationNewMessages.contentIntent);
					notifications.notify(notifId, notificationNewMessages); 
				}
				else
				{
					final long forumId = lastReceivedMessage.getForumId();
					final Intent intent = new Intent(AnusaiService.this, MessagesActivity.class);
					intent.putExtra("forumId", forumId);
					notificationNewMessages.contentIntent = PendingIntent.getActivity(ctx, notifId, intent, 0);
					notificationNewMessages.setLatestEventInfo(ctx, notificationTitle, 
							newMsgNotificationText + " (" + notificationNewMessages.number + ")",
							 notificationNewMessages.contentIntent);
					notifications.notify(notifId, notificationNewMessages); 
				}
			}
		});
		newMessageCount.addChangeListener(newMessageCountListener);
		startForeground(NOTIFICATION_FOREGROUND_ID, notificationForeground);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startRefresh();
		return Service.START_NOT_STICKY;
	}

	public static void updateAlarms() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				final Context ctx = RSDNApplication.getInstance().getApplicationContext();
				final AlarmManager alarms = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
				final Intent intent = new Intent(MessageRefreshActionReceiver.ACTION_REFRESH_MESSAGES);
				final PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

				boolean autoUpdate = true;
				if (autoUpdate) {
					final long interval = 5 * 60 * 1000;
					final long timeToRefresh = SystemClock.elapsedRealtime();
					alarms.setRepeating(AlarmManager.ELAPSED_REALTIME, timeToRefresh, interval, alarmIntent);
					alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToRefresh,
							AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
				} else {
					alarms.cancel(alarmIntent);
				}
			}});
	}

	private void startRefresh() {
		if (asyncTask == null) {
			asyncTask = new MessagesRefreshTask2().execute();
		}
	}
	
	@Override
	public void onDestroy() {
		if (asyncTask != null) {
			asyncTask.cancel(true);
			asyncTask = null;
		}
		super.onDestroy();
	}	

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private final class MessagesRefreshTask2 extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				refreshMessages();
			} catch (Exception e) {
				Log.e(TAG, "Error in refreshMessages", e);
			}
			return null;
		}
	}

	private void refreshMessages() {
		try {
			final RSDNApplication app = RSDNApplication.getInstance();
			final RsdnWebService ws = app.getWebService();
			int numTries = 0;
			while (numTries++ < 3) {
				try {
					ws.postChange.call(null);
					ws.postChangeCommit.call(null);
					ws.getNewUsers.call(new EntityReceiver<UserEntity>() {
						final LinkedList<User> accUsers = new LinkedList<User>();
						@Override
						public boolean consume(final UserEntity result) {
							accUsers.add((User)result);
							if (accUsers.size() > RSDNApplication.CACHED_ENTITIES_SIZE)
								flush();
							return true;
						}
						@Override
						public void flush() {
							app.getUsers().putAll(accUsers);
							accUsers.clear();
						}
					});
					
					final EntityReceiver<MessageEntity> messageRecv = new EntityReceiver<MessageEntity>() {
						final LinkedList<Message> accMessages = new LinkedList<Message>();
						@Override
						public boolean consume(final MessageEntity result) {
							accMessages.add((Message) result);
							if (accMessages.size() > RSDNApplication.CACHED_ENTITIES_SIZE)
								flush();
							return true;
						}
						@Override
						public void flush() {
							final int size = accMessages.size();
							if (size != 0) {
								AnusaiService.this.lastReceivedMessage = accMessages.get(size - 1);
								newMessageCount.set(newMessageCount.get() + size);
								app.getMessages().putAll(accMessages);
								accMessages.clear();
							}
						}
					};
					ws.getNewData.call(messageRecv);
					ws.getBrokenTopics.call(messageRecv);
					break;
				} catch(final Exception e) {
					Log.e(TAG, "Error inside of refreshMessages", e);
					final String toastMsg = String.format("Try #%d  Error: %s", numTries, e.getLocalizedMessage());
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
						}});
					try {
						Thread.sleep(20000);
					} catch (Exception e1) {
						break;
					}
				}
			}
		} finally {
			RSDNApplication.getInstance().flushData();
			handler.post(new Runnable() {
				@Override
				public void run() {
					broadcastNewDataNotification();
					AnusaiService.this.stopSelf();
				}});
		}
	}

	private void broadcastNewDataNotification() {
		final Intent notificationIntent = new Intent(NEW_DATA_RECEIVED);
		sendBroadcast(notificationIntent);
	}
	
	
}
