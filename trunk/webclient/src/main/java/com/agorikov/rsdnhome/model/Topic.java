package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public final class Topic {
	private final long topicId;
	private final long lastMsgId;
	private final int size;
	
	private Topic(final long topicId, final long lastMsgId, final int size) {
		this.topicId = topicId;
		this.lastMsgId = lastMsgId;
		this.size = size;
	}
	
	public long getId() {
		return this.topicId;
	}
	public long getLastMessageId() {
		return this.lastMsgId;
	}
	public int getSize() {
		return this.size;
	}
	
//	private final Message firstMessage;
//	private final Message lastMessage;
//	private final int rating;
//	private final int size;
//	
//	public Message getFirstMessage() {
//		return firstMessage;
//	}
//	
//	public Message getLastMessage() {
//		return lastMessage;
//	}
//	
//	public int getSize() {
//		return size;
//	}
//	
//	public int getRating() {
//		return rating;
//	}
//	
//	private Topic(final Message firstMessage, final Message lastMessage, final int size, final int rating) {
//		this.firstMessage = firstMessage;
//		this.lastMessage = lastMessage;
//		this.size = size;
//		this.rating = rating;
////		if (firstMessage.getTopicId() != lastMessage.getTopicId())
////			throw new RuntimeException("firstMessage.topicId != lastMessage.topicId");
//	}
//	
//	@Override
//	public String toString() {
//		return String.format("%s   (%s)%s : %s(%s) : (%s)%s: %s:%s(%s) : topic size: %d", firstMessage.getTopicId(),
//				firstMessage.getId(), firstMessage.getSubject(), 
//				firstMessage.getUserId(), firstMessage.getUserName(), 
//				lastMessage.getId(), lastMessage.getSubject(), lastMessage.getMessageDate(), lastMessage.getUserId(), lastMessage.getUserName(),
//				getSize());
//	}
	
	public static class TopicBuilder implements Builder<Topic> {
		private long topicId;
		private long lastMsgId;
		private int size;
		
//		private Message firstMessage;
//		private Message lastMessage;
//		private int size;
//		private int rating;
		
		public static TopicBuilder create() {
			return new TopicBuilder();
		}

		void reset() {
			this.topicId = -1;
			this.lastMsgId = -1;
			this.size = 0;
			
//			this.firstMessage = null;
//			this.lastMessage = null;
//			this.size = 0;
//			this.rating = 0;
		}

		public final TopicBuilder topicId(final long topicId) {
			this.topicId = topicId;
			return this;
		}
		
		public final TopicBuilder lastMsgId(final long lastMessageId) {
			this.lastMsgId = lastMessageId;
			return this;
		}
		
		public final TopicBuilder size(final int size) {
			this.size = size;
			return this;
		}
		
//		final TopicBuilder firstMessage(final Message firstMessage) {
//			this.firstMessage = firstMessage;
//			return this;
//		}
//		
//		final TopicBuilder lastMessage(final Message lastMessage) {
//			this.lastMessage = lastMessage;
//			return this;
//		}
//		
//		final TopicBuilder size(final int size) {
//			this.size = size;
//			return this;
//		}
//		
//		final TopicBuilder rating(final int rating) {
//			this.rating = rating;
//			return this;
//		}
		
		@Override
		public Topic build() {
			try {
				return new Topic(topicId, lastMsgId, size);
				//return new Topic(firstMessage, lastMessage, size, rating);
			} finally {
				reset();
			}
		}
	}
	
	
}
