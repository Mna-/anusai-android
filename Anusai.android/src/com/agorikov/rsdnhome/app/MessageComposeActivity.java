package com.agorikov.rsdnhome.app;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.common.MessageEditText;
import com.agorikov.rsdnhome.common.MessageViewFormatter;
import com.agorikov.rsdnhome.common.Strings;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.User;

public class MessageComposeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.message_compose);
		super.onCreate(savedInstanceState);
		fillControlBar();
	
		attachStyleButtons();
		
		final EditText subjEdit = (EditText) 
				findViewById(R.id.subjectEdit);
		subjEdit.setText(formatSubjectTitle());
		
		final ImageButton saveButton = (ImageButton) findViewById(R.id.saveBtn);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent data = new Intent();
				final EditText subjEdit = (EditText) findViewById(R.id.subjectEdit);
				final String subj = subjEdit.getText().toString();
				data.putExtra("subj", subj);

				final MessageEditText messageBodyEdit = (MessageEditText) 
						findViewById(R.id.messageBodyEdit);
				final String markup = messageBodyEdit.readOnlyMarkup().get();
				data.putExtra("messageBody", markup);
				setResult(RESULT_OK, data);
				finish();
			}
		});
		
	}

	private List<ChangeListener<Boolean>> styleChangeListeners;
	
	private void fillControlBar() {
		final RelativeLayout controlBar = (RelativeLayout) findViewById(R.id.controlBar);
		
		final Resources res = getResources();
		final float density = res.getDisplayMetrics().density;
		int index = controlBar.getChildCount();
		int prevViewId = controlBar.getChildAt(index - 1).getId();
		int bnId = prevViewId + 1000;
		final int padding = (int) (6 * density + 0.5);
		final int marginLeft = (int) (6 * density + 0.5);

		final MessageEditText messageBodyEdit = (MessageEditText) 
				findViewById(R.id.messageBodyEdit);
		
		for (final String emoticon : res.getString(R.string.controlPanel).split("\\s{1,}")) {
			final Button btn = new Button(this);
			btn.setId(bnId);
			btn.setText(MessageViewFormatter.format(emoticon, btn));
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final int selStart = messageBodyEdit.getSelectionStart();
					final int selEnd = messageBodyEdit.getSelectionEnd();
					final Editable editable = messageBodyEdit.getText();
					if (selStart <= selEnd)
						editable.replace(selStart, selEnd, emoticon);
					else
						editable.replace(selEnd, selStart, emoticon);
				}});
			btn.setPadding(padding, 0, padding, 0);
			btn.setBackgroundDrawable(res.getDrawable(R.drawable.button_toggle));
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT);
			params.addRule(RelativeLayout.RIGHT_OF, prevViewId);
			params.leftMargin = marginLeft;
			controlBar.addView(btn, index, params);
			prevViewId = bnId++;
			++index;
		}
	}
	
	private void attachStyleButtons() {
		final RelativeLayout controlBar = (RelativeLayout) findViewById(R.id.controlBar);
		final MessageEditText messageBodyEdit = (MessageEditText) 
				findViewById(R.id.messageBodyEdit);
		styleChangeListeners = new LinkedList<ChangeListener<Boolean>>();

		attachStyleButton(controlBar, R.id.bold, messageBodyEdit.boldProperty());
		attachStyleButton(controlBar, R.id.em, messageBodyEdit.emProperty());
		attachStyleButton(controlBar, R.id.underline, messageBodyEdit.underlineProperty());
		attachStyleButton(controlBar, R.id.strike, messageBodyEdit.strikeProperty());
	}

	private void attachStyleButton(final RelativeLayout controlBar,
			final int id, final Property<Boolean> selected) {
		final ToggleButton tb = (ToggleButton) controlBar.findViewById(id);
		final ChangeListener<Boolean> listener = new ChangeListener<Boolean> () {
			@Override
			public void onChange(Observable bean, Boolean oldValue,
					final Boolean selected) {
				tb.setChecked(selected);
			}};
		styleChangeListeners.add(listener);
		selected.addChangeListener(listener);
		listener.onChange(selected, null, selected.get());
		tb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				selected.set(isChecked);
			}});
	}
	
	private final static Pattern patRe = Pattern.compile("\\s*Re\\s*(?:(?:\\[\\s*(\\d{1,})\\s*\\])|())\\s*:");

	private String formatSubjectTitle() {
		final long messageId = getIntent().getLongExtra("messageId", -1);
		
		final RSDNApplication app = RSDNApplication.getInstance();
		final Message message = app.getMessages().get(messageId);
		if (message == null)
			return "";
		
		final String subj = Converters.nonNullStr(message.getSubject());
		final StringBuilder sb = new StringBuilder();
		final Matcher m = patRe.matcher(subj);
		if (m.find()) {
			final String levelStr = m.group(1);
			if (levelStr != null && !levelStr.equals("")) {
				final int level = Integer.parseInt(levelStr);
				sb.append(subj.substring(0, m.start(1)))
					.append(level + 1)
					.append(subj.substring(m.end(1)));
			} else {
				sb.append("Re[2]:").append(subj.substring(m.end()));
			}
		} else {
			sb.append("Re: ").append(subj);
		}
		
		return sb.toString();
	}
	
	private String formatAnswerMarkup() {
		final long messageId = getIntent().getLongExtra("messageId", -1);
		
		final RSDNApplication app = RSDNApplication.getInstance();
		final Message message = app.getMessages().get(messageId);
		if (message == null)
			return "";
		final StringBuilder answerMarkup = new StringBuilder();
		
		final Long userId = message.getUserId();
		final User user = app.getUsers().get(userId != null ? userId : -1);
		final String userName = user != null ? user.getName() : String.valueOf(userId);
		final String initials = Strings.initials(Converters.nonNullStr(userName));
		
		answerMarkup.append(String.format(getResources().getString(R.string.youWrote), userName));
		answerMarkup.append("\r\n\r\n");
		
		final String body = Converters.nonNullStr(message.getBody())
				.replaceAll("$*\\s*(\\[tagline\\].*\\[/tagline\\]?)\\s*", "");
		for (String line : body.split("\\r?\\n")) {
			final Matcher m = MessageViewFormatter.replyPattern.matcher(line);
			if (m.find()) {
				//final String prevInitials = m.group(1);
				answerMarkup.append(line.substring(0, m.end(2)))
					.append(">")
					.append(line.substring(m.end(2)));
			} else if (line.length() != 0) {
				answerMarkup.append(initials)
					.append(">")
					.append(line);
			}
			answerMarkup.append("\r\n");
		}
		answerMarkup.append("\r\n");

		return answerMarkup.toString();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		final Intent intent = getIntent();

		final EditText subjEdit = (EditText) findViewById(R.id.subjectEdit);
		intent.putExtra("subj", subjEdit.getText());

		final MessageEditText messageBodyEdit = (MessageEditText) 
				findViewById(R.id.messageBodyEdit);
		final String markup = messageBodyEdit.readOnlyMarkup().get();
		intent.putExtra("messageBody", markup);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final Intent intent = getIntent();
		
		final EditText subjEdit = (EditText) findViewById(R.id.subjectEdit);
		final String subj = intent.getStringExtra("subj");
		if (subj != null && !subj.equals("")) 
			subjEdit.setText(subj);
		else
			subjEdit.setText(formatSubjectTitle());
		
		final MessageEditText messageBodyEdit = (MessageEditText) 
				findViewById(R.id.messageBodyEdit);
		final String markup = intent.getStringExtra("messageBody");
		if (markup == null) {
			messageBodyEdit.setMarkup(formatAnswerMarkup());
		} else {
			messageBodyEdit.setMarkup(markup);
		}
	}
	
}
