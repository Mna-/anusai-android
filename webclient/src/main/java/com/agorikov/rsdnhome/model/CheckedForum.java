package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public class CheckedForum extends Forum {

	private final boolean checked;
	
	protected CheckedForum(long id, Long groupId, String shortName,
			String fullName, boolean checked) {
		super(id, groupId, shortName, fullName);
		this.checked = checked;
	}

	public final boolean isChecked() {
		return checked;
	}
	
	protected static class CheckedForumBuilderImpl<B extends Builder<CheckedForum>> extends ForumBuilderImpl<CheckedForumBuilderImpl<B>> {
		protected boolean checked;
		
		@SuppressWarnings("unchecked")
		public final B checked(final boolean checked) {
			this.checked = checked;
			return (B) this;
		}
		public final CheckedForum build() {
			try {
				return new CheckedForum(id, groupId, shortName, fullName, checked);
			} finally {
				reset();
			}
		}
	}
	
	public static class CheckedForumBuilder extends CheckedForumBuilderImpl<CheckedForumBuilder> implements Builder<CheckedForum> {
		public static CheckedForumBuilder create() {
			return new CheckedForumBuilder();
		}
	}
	
}
