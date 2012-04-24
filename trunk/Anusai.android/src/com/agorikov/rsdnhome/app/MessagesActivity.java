package com.agorikov.rsdnhome.app;

import java.text.DateFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import com.agorikov.rsdnhome.common.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.common.MessageViewFormatter;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.Topic;
import com.agorikov.rsdnhome.model.Topics;

public final class MessagesActivity extends Activity {
	static final String TAG = "MessagesActivity";
	static final int PICK_FORUM = 0;
	static final int SHOW_MESSAGE = 1;
	static final int COMPOSE_MESSAGE = 2;
	static final Handler handler = new Handler();
	private AsyncTask<Void,Void,Void> messagesRefreshTask;
	
	private long forumId;
	
	private static class MenuItemIds {
		final static int refreshData = Menu.FIRST;
		final static int forums = Menu.FIRST + 1;
		final static int credentials = Menu.FIRST + 2;
		final static int about = Menu.FIRST + 3;
		final static int compose = Menu.FIRST + 4;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.messages);
		final SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
		final Set<Long> availableForumIds = new LinkedHashSet<Long>();
		for (final Forum forum : RSDNApplication.getInstance().getForums().getAvailable()) {
			availableForumIds.add(forum.getId());
		}
		this.forumId = -1;
		
		long forumId = getIntent().getLongExtra("forumId", -1);
		if (forumId == -1) {
			forumId = pref.getLong("forumId", this.forumId);
			if (!availableForumIds.contains(forumId)) {
				forumId = !availableForumIds.isEmpty() ? availableForumIds.iterator().next() : -1;
			}
		}
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.messages_header);		
		final Button bnSelectForum = (Button) getWindow().getDecorView().findViewById(R.id.messagesHeaderButtonId);
		bnSelectForum.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "bn clicked");
				final Intent i = new Intent(MessagesActivity.this, ForumSelectorActivity.class);
				i.putExtra("forumId", MessagesActivity.this.forumId);
				startActivityForResult(i, PICK_FORUM);
			}
		});
		
		super.onCreate(savedInstanceState);
		
		final ListView listView = (ListView) findViewById(R.id.message_list);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int pos, final long itemId) {
				@SuppressWarnings("unchecked")
				final ArrayAdapter<Long> adapter = (ArrayAdapter<Long>)parent.getAdapter();
				activateTopicMessage(adapter.getItem(pos));
			}});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int pos, long itemId) {
				@SuppressWarnings("unchecked")
				final ArrayAdapter<Long> adapter = (ArrayAdapter<Long>)parent.getAdapter();
				activateMessage(adapter.getItem(pos));
				return true;
			}
		});
		
		final RSDNApplication app = RSDNApplication.getInstance();
		final Topics topics = app.getTopics();
		topics.busyProperty().addChangeListener(busyListener);
		busyListener.onChange(null, null, topics.busyProperty().get());
		setForumId(forumId);
		registerReceiver(receiver, new IntentFilter(AnusaiService.NEW_DATA_RECEIVED));
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	private final ChangeListener<Boolean> busyListener = new ChangeListener<Boolean>() {
		@Override
		public void onChange(final Observable bean, final Boolean oldValue, final Boolean newValue) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					final View progrBar = getWindow().getDecorView().findViewById(R.id.progressBar1);
					progrBar.setVisibility(newValue ? View.VISIBLE : View.INVISIBLE);
				}});
		}};

	protected void activateMessage(final long msgId) {
		Log.d(TAG, String.format("Clicked msg #%d", msgId));
		final Intent i = new Intent(MessagesActivity.this, MessageViewActivity.class);
		i.putExtra("messageId", msgId);
		startActivity(i);
	}


	protected void activateTopicMessage(final long msgId) {
		final Message message = RSDNApplication.getInstance().getMessages().get(msgId);
		if (message != null) {
			final long topicId = message.getTopicId();
			Log.d(TAG, String.format("Clicked msg #%d", topicId));
			final Intent i = new Intent(MessagesActivity.this, MessageViewActivity.class);
			i.putExtra("messageId", topicId);
			startActivity(i);
		}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICK_FORUM:
			if (resultCode == RESULT_OK) {
				final long forumId = data.getLongExtra("forumId", -1);
				if (forumId >= 0 && forumId != this.forumId) {
					final Intent i = new Intent(this, MessagesActivity.class);
					i.putExtra("forumId", forumId);
					startActivity(i);
				}
			}
			break;
		case COMPOSE_MESSAGE:
			if (resultCode == RESULT_OK) {
				final String subj = data.getStringExtra("subj");
				final String body = data.getStringExtra("messageBody");
				RSDNApplication.getInstance().getComposedMessages().putAll(MessageComposeUtils
						.composeMessage(subj, body, 0, forumId));
				startService(new Intent(this, AnusaiService.class));
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	

	private void setForumId(long forumId) {
		if (forumId == -1 || this.forumId != forumId) {
			final RSDNApplication app = RSDNApplication.getInstance();
			this.forumId = forumId;
			
			final Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
			editor.putLong("forumId", this.forumId);
			editor.commit();
			
			getIntent().putExtra("forumId", forumId);
			final Forum forum = app.getForums().get(forumId);
			final Button bnSelectForum = (Button) getWindow().getDecorView().findViewById(R.id.messagesHeaderButtonId);
			if (forum != null)
				bnSelectForum.setText(forum.getFullName());
			else
				bnSelectForum.setText(R.string.forumNotSelected);
			refreshView();
		}
	}

	private void refreshView() {
		if (paused) {
			pendingRefresh = true;
			return;
		}
		pendingRefresh = false;
		
		if (messagesRefreshTask != null) {
			messagesRefreshTask.cancel(false);
			messagesRefreshTask = null;
		}
		handler.post(new Runnable() {
			@Override
			public void run() {
				final ListView view = (ListView) findViewById(R.id.message_list);
				view.setAdapter(new ArrayAdapter<Topic>(MessagesActivity.this, 0, Collections.<Topic>emptyList()));
			}
		});
		messagesRefreshTask = new MessagesRefreshTask().execute();
	}
	
	protected void updateForumMessages() {
		final RSDNApplication app = RSDNApplication.getInstance();
		final Topics topics = app.getTopics();
		//final long[] lastMsgIds = topics.getFromForumFast(forumId);
		final List<Long> topicList = Converters.asArrayList(topics.getFromForum(forumId));
		handler.post(new Runnable() {
			@Override
			public void run() {
				final Context ctx = getApplicationContext();
				final DateFormat dtFormatter = android.text.format.DateFormat.getDateFormat(ctx);
				final DateFormat tmFormatter = android.text.format.DateFormat.getTimeFormat(ctx);
				messagesRefreshTask = null;
				final ListView view = (ListView) findViewById(R.id.message_list);
				final ArrayAdapter<Long> adapter = new ArrayAdapter<Long>(MessagesActivity.this, 0, topicList) {
					@Override
					public View getView(final int position, final View convertView, final ViewGroup parent) {
						final TwoLineListItem view;
						if (convertView == null) {
							final LayoutInflater inflater = getLayoutInflater();
							view = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
						} else {
							view = (TwoLineListItem) convertView;
						}
						
						final TextView primaryText = view.getText1();
						primaryText.setTextColor(0xff000000);
						
						final Message[] msg = topics.readTopic(getItem(position));
						
						primaryText.setText(MessageViewFormatter.format(msg[0].getSubject(), primaryText));
						
						final TextView secondaryText = view.getText2();
						secondaryText.setTextColor(0xa0000000);
						
						final String markupTopicItem = String.format(ctx.getString(R.string.topicItemFormat),
								msg[0].getUserName(),
								Converters.safeFormatDate(dtFormatter, msg[0].getMessageDate()),
								Converters.safeFormatDate(tmFormatter, msg[0].getMessageDate()),
								msg[1].getUserName(),
								Converters.safeFormatDate(dtFormatter, msg[1].getMessageDate()),
								Converters.safeFormatDate(tmFormatter, msg[1].getMessageDate()));
						secondaryText.setText(MessageViewFormatter.format(markupTopicItem, secondaryText));
						
						return view;
					}
				};
				view.setAdapter(adapter);
			}});
	}

	private final class MessagesRefreshTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				updateForumMessages();
				//refreshMessages();
			} catch (Exception e) {
				Log.e(TAG, "Error in updateForumMessages", e);
			}
			return null;
		}
	}

	private void launchForumList() {
		final Intent i = new Intent(this, ForumsActivity.class);
		startActivity(i);
	}

	private void launchCompose() {
		final Intent i = new Intent(this, MessageComposeActivity.class);
		i.putExtra("forumId", forumId);
		startActivityForResult(i, COMPOSE_MESSAGE);
	}
	
	private void launchCredentials() {
		final Intent i = new Intent(this, CredentialsActivity.class);
		startActivity(i);
	}

	private void launchAbout() {
		final Intent i = new Intent(this, AboutActivity.class);
		startActivity(i);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MenuItemIds.refreshData, Menu.NONE, R.string.refreshData).setIcon(R.drawable.ic_menu_refresh);
		menu.add(0, MenuItemIds.compose, Menu.NONE, R.string.compose).setIcon(R.drawable.ic_menu_compose);
		menu.add(0, MenuItemIds.forums, Menu.NONE, R.string.forums).setIcon(R.drawable.ic_menu_archive);
		menu.add(0, MenuItemIds.credentials, Menu.NONE, R.string.credentials).setIcon(R.drawable.ic_menu_login);
		menu.add(0, MenuItemIds.about, Menu.NONE, R.string.about).setIcon(R.drawable.ic_menu_help);
		
		return true;
	}
	
	final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshView();
		}
	};
	private boolean paused;
	private boolean pendingRefresh;
	
	
	@Override
	protected void onPause() {
		this.paused = true;
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		this.paused = false;
		if (pendingRefresh) {
			refreshView();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MenuItemIds.refreshData:
			startService(new Intent(this, AnusaiService.class));
			return true;
		case MenuItemIds.forums:
			launchForumList();
			return true;
		case MenuItemIds.credentials:
			launchCredentials();
			return true;
		case MenuItemIds.about:
			launchAbout();
			return true;
		case MenuItemIds.compose:
			launchCompose();
			return true;
		default:;
		}
		return false;
	}
	
}
