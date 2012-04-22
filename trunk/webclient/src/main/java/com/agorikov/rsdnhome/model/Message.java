package com.agorikov.rsdnhome.model;

import java.util.Date;

import com.agorikov.rsdnhome.common.Builder;

public final class Message implements MessageEntity {

	private final long id;
	private final long topicId;
	private final Long parentId;
	private final Long userId;
	private final long forumId;
	private final String subject;
	private final String body;
	private final Long articleId;
	private final Date messageDate;
	private final Date lastModerated;
	private final String userName;
	

	private Message(final long id, final long topicId,
			final Long parentId, final Long userId, final long forumId,
			final String subject, final String body, final Long articleId,
			final Date messageDate, final Date lastModerated, final String userName) {
		this.id = id;
		this.topicId = topicId;
		this.parentId = parentId;
		this.userId = userId;
		this.forumId = forumId;
		this.subject = subject;
		this.body = body;
		this.articleId = articleId;
		this.messageDate = messageDate;
		this.lastModerated = lastModerated;
		this.userName = userName;
	}
	
	public long getId() {
		return id;
	}
	
	public long getTopicId() {
		return topicId;
	}
	
	public Long getParentId() {
		return parentId;
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public long getForumId() {
		return forumId;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getBody() {
		return body;
	}
	
	public Long getArticleId() {
		return articleId;
	}
	
	public Date getMessageDate() {
		return messageDate;
	}
	
	public Date getLastModerated() {
		return lastModerated;
	}
	
	public String getUserName() {
		return userName;
	}
	
	@Override
	public String toString() {
		return String.format("Id: %d(%d);    Author: %s(%s); Sent: {%s};  Subject: {%s};  Body: {%s}", getId(), getTopicId(), getUserName(), getUserId(), getMessageDate(), getSubject(), getBody());
	}
	
	public static abstract class MessageBuilder implements Builder<Message> {
		
		public static MessageBuilder create() {
			return new MessageBuilder() {};
		}
		
		private Long id;
		private Long topicId;
		private Long parentId;
		private Long userId;
		private Long forumId;
		private String subject;
		private String body;
		private Long articleId;
		private Date messageDate;
		private Date lastModerated;
		private String userName;
		
		void reset() {
			this.id = null;
			this.topicId = null;
			this.parentId = null;
			this.userId = null;
			this.forumId = null;
			this.subject = null;
			this.body = null;
			this.articleId = null;
			this.messageDate = null;
			this.lastModerated = null;
			this.userName = null;
		}
		
		public final MessageBuilder id(final long id) {
			this.id = id;
			return this;
		}
		
		public final MessageBuilder topicId(final Long topicId) {
			this.topicId = topicId;
			return this;
		}
		
		public final MessageBuilder parentId(final Long parentId) {
			this.parentId = parentId;
			return this;
		}
		
		public final MessageBuilder userId(final Long userId) {
			this.userId = userId;
			return this;
		}
		
		public final MessageBuilder forumId(final Long forumId) {
			this.forumId = forumId;
			return this;
		}
		
		public final MessageBuilder subject(final String subject) {
			this.subject = subject;
			return this;
		}
		
		public final MessageBuilder body(final String body) {
			this.body = body;
			return this;
		}
		
		public final MessageBuilder articleId(final Long articleId) {
			this.articleId = articleId;
			return this;
		}
		
		public final MessageBuilder messageDate(final Date messageDate) {
			this.messageDate = messageDate;
			return this;
		}
		
		public final MessageBuilder lastModerated(final Date lastModerated) {
			this.lastModerated = lastModerated;
			return this;
		}
		
		public final MessageBuilder userName(final String userName) {
			this.userName = userName;
			return this;
		}
		
		
		@Override
		public Message build() {
			try {
				return new Message(id, topicId != 0 ? topicId : id, parentId, userId, forumId, 
						subject, body, articleId, messageDate, lastModerated, userName);
			} catch (RuntimeException e) {
				throw e;
			} finally {
				reset();
			}
		}
		
	}
	
	
}
