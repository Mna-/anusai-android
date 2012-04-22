package com.agorikov.rsdnhome.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.agorikov.rsdnhome.app.R;
import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.ForumGroup;

public class ForumSelectorActivity extends Activity {
    public static final String TAG = "ForumSelectorActivity";

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum_selector);
		final ListView forumsView = (ListView)findViewById(R.id.forumList);
		forumsView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int pos, final long itemId) {
				@SuppressWarnings("unchecked")
				final ArrayAdapter<Forum> adapter = (ArrayAdapter<Forum>) parent.getAdapter();
				final Forum forum = adapter.getItem(pos);
				final Intent data = new Intent();
				data.putExtra("forumId", forum != null ? forum.getId() : 0);
				setResult(RESULT_OK, data);
				finish();
			}
		});
		
		updateForums();
	}
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	updateForums();
    }
    
    private void updateForums() {
		final ListView forumsView = (ListView)findViewById(R.id.forumList);
    	final RSDNApplication app = RSDNApplication.getInstance();
    	final List<Forum> forums = Converters.asArrayList(app.getForums().getAvailable());
    	final Map<Long, ForumGroup> groupMap = new HashMap<Long, ForumGroup>();
    	for (final ForumGroup gr : app.getForumGroups().getAll()) {
    		groupMap.put(gr.getId(), gr);
    	}
		final ArrayAdapter<Forum> adapter = new ArrayAdapter<Forum>(ForumSelectorActivity.this,
				0, forums) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
		        final LayoutInflater inflater= getLayoutInflater(); 
		        final TwoLineListItem view = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null); 
				final Forum forum = getItem(position);
				final TextView text1 = view.getText1();
				text1.setTextColor(0xff000000);
				text1.setText(forum.getFullName());
				
				final TextView text2 = view.getText2();
				text2.setTextColor(0xa0000000);
				final ForumGroup gr = groupMap.get(forum.getGroupId());
				text2.setText(gr != null ? gr.getName() : "");
		        return view;
			}
		};
		forumsView.setAdapter(adapter);
    }
}
