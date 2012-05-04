package com.agorikov.rsdnhome.app;

import java.text.DateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.common.MessageViewFormatter;
import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.Messages;

public class MessageViewActivity extends Activity {
	static final String TAG = "MessageViewActivity";
	static final int COMPOSE_MESSAGE = 0;

	private long messageId;
	private Runnable updateChildMessagesRunnable;
	private boolean paused;
	private boolean pendingRefresh;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshView();
		}
	};


	private static class MenuItemIds {
		final static int postReply = Menu.FIRST;
		final static int jumpToParent = Menu.FIRST + 1;
	}	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.message_view);
		messageId = getIntent().getLongExtra("messageId", 0);
		
		final Message message = RSDNApplication.getInstance().getMessages().get(messageId);
		
		super.onCreate(savedInstanceState);
		if (message != null)
			fillMessageView(message);
		registerReceiver(receiver, new IntentFilter(AnusaiService.NEW_DATA_RECEIVED));
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	private void fillMessageView(final Message message) {
		final Context ctx = getApplicationContext();
		final DateFormat dtFormatter = android.text.format.DateFormat.getDateFormat(ctx);
		final DateFormat tmFormatter = android.text.format.DateFormat.getTimeFormat(ctx);
		
		final TextView messageHeader = (TextView) findViewById(R.id.messageTopicHeader);
		final String markupHeaderItem = String.format(ctx.getString(R.string.messageHeaderFormat), 
				message.getUserName(), dtFormatter.format(message.getMessageDate()),
				tmFormatter.format(message.getMessageDate()));

		final SpannableStringBuilder formattedHeaderText = MessageViewFormatter.format(message.getSubject() + "\n" + markupHeaderItem,
				messageHeader);
		formattedHeaderText.setSpan(new StyleSpan(Typeface.BOLD), 0, 
				formattedHeaderText.toString().indexOf('\n') + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		messageHeader.setText(formattedHeaderText);

		messageHeader.setMovementMethod(LinkMovementMethod.getInstance());
		final ImageButton backButton = (ImageButton) findViewById(R.id.backButton);
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				launchViewParent(message);
			}
		});

		final ImageButton composeButton = (ImageButton) findViewById(R.id.composeButton);
		composeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				launchMessageCompose();
			}});
		
		final ListView childMessages = (ListView) findViewById(R.id.childNodes);
		childMessages.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> view, View arg1, int position,
					long arg3) {
				final Long messageId = (Long) view.getAdapter().getItem(position);
				activateMessage(messageId);
			}
		});
		
		
		final LayoutInflater inflater = getLayoutInflater();
		final View messageBody = inflater.inflate(R.layout.message_body, childMessages, false);
		final TextView messageView = (TextView)messageBody.findViewById(R.id.messageBody) ;
		messageView.setMovementMethod(LinkMovementMethod.getInstance());
		messageView.setText(MessageViewFormatter.format(message.getBody(), messageView));
		childMessages.addHeaderView(messageBody, null, false);
	
		this.updateChildMessagesRunnable = new Runnable() {
			@Override
			public void run() {
				updateChildMessages(ctx, dtFormatter, tmFormatter, childMessages,
						inflater, messageBody);
			}
		};
		refreshView();
	}

	private void updateChildMessages(final Context ctx,
			final DateFormat dtFormatter, final DateFormat tmFormatter,
			final ListView childMessages, final LayoutInflater inflater,
			final View messageBody) {
		final Messages messages = RSDNApplication.getInstance().getMessages();
		final ArrayList<Long> childMsgIds = Converters.<Long>asArrayList(messages
				.getFromParent(messageId));
		messageBody.findViewById(R.id.messageBodyDelimiter).setVisibility(!childMsgIds.isEmpty() ? View.VISIBLE : View.GONE);
		
		final ArrayAdapter<Long> adapter = new ArrayAdapter<Long>(MessageViewActivity.this, 0, 
				childMsgIds) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				final TwoLineListItem view = convertView != null ? (TwoLineListItem) convertView 
						: (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
				
				final Message msg = messages.get(childMsgIds.get(position));
				
				final TextView primaryText = view.getText1();
				primaryText.setTextColor(0xff000000);
				primaryText.setText(MessageViewFormatter.format(msg.getSubject(), primaryText));

				final TextView secondaryText = view.getText2();
				secondaryText.setTextColor(0xa0000000);
				final String markupHeaderItem = String.format(ctx.getString(R.string.messageHeaderFormat), 
						msg.getUserName(),
						dtFormatter.format(msg.getMessageDate()),
						tmFormatter.format(msg.getMessageDate()));
				secondaryText.setText(MessageViewFormatter.format(markupHeaderItem, secondaryText));
				return view;
			}
		};
		childMessages.setAdapter(adapter);
	}

	protected void activateMessage(final long msgId) {
		Log.d(TAG, String.format("Clicked msg #%d", msgId));
		final Intent i = new Intent(MessageViewActivity.this, MessageViewActivity.class);
		i.putExtra("messageId", msgId);
		startActivity(i);
	}
	
	private void launchViewParent(final Message message) {
		if (message.getTopicId() != message.getId() && message.getParentId() != null)
			activateMessage(message.getParentId());
		else
			activateMessages(message.getForumId());
	}
	protected void activateMessages(final long forumId) {
		Log.d(TAG, String.format("Clicked forum #%d", forumId));
		final Intent i = new Intent(MessageViewActivity.this, MessagesActivity.class);
		i.putExtra("forumId", forumId);
		startActivity(i);
	}

	private void launchMessageCompose() {
		final Intent i = new Intent(this, MessageComposeActivity.class);
		i.putExtra("messageId", messageId);
		startActivityForResult(i, COMPOSE_MESSAGE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MenuItemIds.jumpToParent, Menu.NONE, R.string.jumpToParent).setIcon(R.drawable.ic_menu_back);
		menu.add(0, MenuItemIds.postReply, Menu.NONE, R.string.postReply).setIcon(R.drawable.ic_menu_compose);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MenuItemIds.postReply: 
			launchMessageCompose();
			return true;
		case MenuItemIds.jumpToParent:
			final Message message = RSDNApplication.getInstance()
				.getMessages().get(messageId);
			if (message != null)
				launchViewParent(message);
			return true;
		}
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		this.paused = false;
		if (pendingRefresh)
			refreshView();
	}
	
	@Override
	protected void onPause() {
		this.paused = true;
		super.onPause();
	}
	
	private void refreshView() {
		if (paused) {
			pendingRefresh = true;
			return;
		}
		pendingRefresh = false;

		if (updateChildMessagesRunnable != null)
			updateChildMessagesRunnable.run();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == COMPOSE_MESSAGE) {
			if (resultCode == RESULT_OK) {
				final String subj = data.getStringExtra("subj");
				final String body = data.getStringExtra("messageBody");
				RSDNApplication.getInstance().getComposedMessages().putAll(MessageComposeUtils
						.composeMessage(subj, body, messageId, 0));
				startService(new Intent(this, AnusaiService.class));
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
}
