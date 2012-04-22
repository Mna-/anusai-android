package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public final class ComposedMessage implements ComposedMessageEntity {

	private final long id;
	private final Long parentId;
	private final Long forumId;
	private final String subj;
	private final String body;
	
	
	private ComposedMessage(final long id, final Long parentId, long forumId,
			final String subj, final String body) {
		this.id = id;
		this.parentId = parentId;
		this.forumId = forumId;
		this.subj = subj;
		this.body = body;
	}
	
	public long getId() {
		return id;
	}
	public Long getParentId() {
		return parentId;
	}
	public long getForumId() {
		return forumId;
	}
	public String getSubj() {
		return subj;
	}
	public String getBody() {
		return body;
	}
	
	public static final class ComposedMessageBuilder implements Builder<ComposedMessage> {
		
		private Long id;
		private Long parentId;
		private Long forumId;
		private String subj;
		private String body;

		public ComposedMessageBuilder id(long id) {
			this.id = id;
			return this;
		}
		public ComposedMessageBuilder parentId(Long parentId) {
			this.parentId = parentId;
			return this;
		}
		public ComposedMessageBuilder forumId(long forumId) {
			this.forumId = forumId;
			return this;
		}
		public ComposedMessageBuilder subj(String subj) {
			this.subj = subj;
			return this;
		}
		public ComposedMessageBuilder body(String body) {
			this.body = body;
			return this;
		}
		
		void reset() {
			this.id = null;
			this.parentId = null;
			this.forumId = null;
			this.subj = null;
			this.body = null;
		}
		
		@Override
		public ComposedMessage build() {
			try {
				return new ComposedMessage(id, parentId, forumId, subj, body);
			} finally {
				reset();
			}
		}
		public static ComposedMessageBuilder create() {
			return new ComposedMessageBuilder();
		}
		
	}

	
	
	
	
	
}
