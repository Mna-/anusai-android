package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public class Forum implements ForumEntity {

	private final long id;
	private final Long groupId;
	private final String shortName;
	private final String fullName;

	protected Forum(final long id, final Long groupId, final String shortName, final String fullName) {
		this.id = id;
		this.groupId = groupId;
		this.shortName = shortName;
		this.fullName = fullName;
	}
	
	public final long getId() {
		return id;
	}
	
	public final Long getGroupId() {
		return groupId;
	}
	
	public final String getShortName() {
		return shortName;
	}
	
	public final String getFullName() {
		return fullName;
	}
	
	@Override
	public String toString() {
		return String.format("%s::%s", getGroupId(), getShortName());
		//return String.format("{%s : %s : %s : %s}", getId(), getGroupId(), getShortName(), getFullName());
	}
	
	
	protected static abstract class ForumBuilderImpl<B extends ForumBuilderImpl<B>> {

		protected Long id;
		protected Long groupId;
		protected String shortName;
		protected String fullName;
		
		void reset() {
			this.id = null;
			this.groupId = null;
			this.shortName = null;
			this.fullName = null;
		}
		
		@SuppressWarnings("unchecked")
		public final B id(final long id) {
			this.id = id;
			return (B)this;
		}
		@SuppressWarnings("unchecked")
		public final B groupId(final Long groupId) {
			this.groupId = groupId;
			return (B) this;
		}
		@SuppressWarnings("unchecked")
		public final B shortName(final String shortName) {
			this.shortName = shortName;
			return (B) this;
		}
		@SuppressWarnings("unchecked")
		public final B fullName(final String fullName) {
			this.fullName = fullName;
			return (B) this;
		}
	}

	public static class ForumBuilder extends ForumBuilderImpl<ForumBuilder> implements Builder<Forum> {
		public static ForumBuilder create() {
			return new ForumBuilder() {};
		}
		public Forum build() {
			try {
				return new Forum(id, groupId, shortName, fullName);
			} finally {
				reset();
			}
		}
	}

	
}
