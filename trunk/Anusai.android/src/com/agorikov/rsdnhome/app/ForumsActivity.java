package com.agorikov.rsdnhome.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.ForumEntity;
import com.agorikov.rsdnhome.model.ForumGroup;
import com.agorikov.rsdnhome.model.ForumGroups;
import com.agorikov.rsdnhome.webclient.model.SimpleWebMethod.EntityReceiver;

public final class ForumsActivity extends BaseAnusaiActivity {
    public static final String TAG = "ForumsActivity";
    AsyncTask<Void, Void, Void> forumsRefreshTask;
    static final Handler handler = new Handler();
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forums);
		
		final ListView forumsView = (ListView)findViewById(R.id.forumList);
		forumsView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View itemView,
					int position, long state) {
				@SuppressWarnings("unchecked")
				final ArrayAdapter<ForumGroup> adapter = (ArrayAdapter<ForumGroup>) parent.getAdapter();
				final ForumGroup group = adapter.getItem(position);
				Log.d(TAG, String.format("Click: %s", group));
				final Intent i = new Intent(ForumsActivity.this, ForumChecksActivity.class);
				i.putExtra("forum_group_id", group.getId());
				i.putExtra("forum_group_name", group.getName());
				startActivity(i);
			}
		});
		
		this.forumsRefreshTask = new ForumsRefreshTask().execute();
	}
	
	private void refreshForums() {
		final RSDNApplication app = RSDNApplication.getInstance();
		app.getWebService().getForumList.call(new EntityReceiver<ForumEntity>() {
			final LinkedList<Forum> accForums = new LinkedList<Forum>();
			final LinkedList<ForumGroup> accForumGroups = new LinkedList<ForumGroup>();
			@Override
			public boolean consume(final ForumEntity result) {
				if (result instanceof Forum)
					accForums.add((Forum) result);
				else if (result instanceof ForumGroup)
					accForumGroups.add((ForumGroup) result);
				
				if (accForums.size() > RSDNApplication.CACHED_ENTITIES_SIZE)
					flushForums();
				if (accForumGroups.size() > RSDNApplication.CACHED_ENTITIES_SIZE)
					flushForumGroups();
				return true;
			}
			private void flushForumGroups() {
				app.getForumGroups().putAll(accForumGroups);
				accForumGroups.clear();
			}
			private void flushForums() {
				app.getForums().putAll(accForums);
				accForums.clear();
			}
			@Override
			public void flush() {
				flushForums();
				flushForumGroups();
			}
		});
		updateForumGroups();
	}

	private void updateForumGroups() {
		final RSDNApplication app = RSDNApplication.getInstance();
		final List<ForumGroup> groups = new ArrayList<ForumGroup>(app.getForumGroups().getAll());
		Collections.sort(groups, ForumGroups.compareByName);
		handler.post(new Runnable() {
		@Override
		public void run() {
			final ListView forumsView = (ListView)findViewById(R.id.forumList);
			final ArrayAdapter<ForumGroup> adapter = new ArrayAdapter<ForumGroup>(ForumsActivity.this, 0, groups) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					final ForumGroup group = getItem(position);
			        final LayoutInflater inflater= getLayoutInflater(); 
			        final LinearLayout row=(LinearLayout) inflater.inflate(R.layout.group_list_item, null); 
			        final TextView label=(TextView)row.findViewById(R.id.group_list_text); 
			        label.setText(group.getName());
			        return row;
				}
			};
			forumsView.setAdapter(adapter);
		}});
	}

	@SuppressWarnings("unused")
	private final ChangeListener<Boolean> forumsBusyListener = new ChangeListener<Boolean>() {
		@Override
		public void onChange(Observable bean, Boolean oldValue, Boolean newValue) {
			Log.d(TAG, "forumsBusyListener was invalidated");
		}
		
	};
	
	
	
	private class ForumsRefreshTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			refreshForums();
			return null;
		}
		
	}
	
	
	
}
