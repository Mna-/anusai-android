package com.agorikov.rsdnhome.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.Forums;
import com.agorikov.rsdnhome.model.Preferences;

public class ForumChecksActivity extends Activity {
	public static final String TAG = "ForumChecksActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		final String forumGroupName = getIntent().getStringExtra("forum_group_name");
		setContentView(R.layout.forums_checks);
		setTitle(forumGroupName);
		super.onCreate(savedInstanceState);
		refreshView();
	}
	   
	@Override
	protected void onRestart() {
		super.onRestart();
		refreshView();
	}

	private void refreshView() {
		final ListView forumsView = (ListView)findViewById(R.id.forumCheckList);
		final long forumGroupId = getIntent().getLongExtra("forum_group_id", 0);
		final Forums forums = RSDNApplication.getInstance().getForums();
		final Preferences preferences = RSDNApplication.getInstance().getPreferences();
		final Set<Long> selectedForumIds = new HashSet<Long>(Converters.asArrayList(preferences.getSelectedForums()));
		final List<Forum> selected = Converters.asArrayList(forums.getFromGroup(forumGroupId));
		final ArrayAdapter<Forum> adapter = new ArrayAdapter<Forum>(ForumChecksActivity.this, android.R.layout.simple_list_item_checked, selected) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
				final Forum forum = getItem(position);
				view.setText(forum.getFullName());
				view.setChecked(selectedForumIds.contains(forum.getId()));
		        return view;
			}
		};
		forumsView.setAdapter(adapter);
		forumsView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View itemView, int position,
					long state) {
				@SuppressWarnings("unchecked")
				final ArrayAdapter<Forum> adapter = (ArrayAdapter<Forum>) parent.getAdapter();
				final long checkedForumId = adapter.getItem(position).getId();
				preferences.selectForum(checkedForumId, !selectedForumIds.contains(checkedForumId));
				ForumChecksActivity.this.finish();
			}
		});
	}
	
}
