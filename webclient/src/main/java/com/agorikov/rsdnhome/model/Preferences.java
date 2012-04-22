package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.SQLException;

public final class Preferences {

	private final LongEntityController selectedForums;
	
	public Preferences(final Connection connection) throws SQLException {
		selectedForums = new LongEntityController(connection, "selected_forums", "forum_id");
		
	}
	
	
	public Iterable<Long> getSelectedForums() {
		return selectedForums.getAll();
	}
	
	public void selectForum(final long forumId, boolean select) {
		if (select)
			selectedForums.putAll(forumId);
		else
			selectedForums.removeAll(forumId);
	}
	
	
}
