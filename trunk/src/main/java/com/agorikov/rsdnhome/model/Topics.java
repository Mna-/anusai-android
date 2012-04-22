package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.model.Message.MessageBuilder;

public final class Topics {

	private final PreparedStatement _getFromForum, _readTopic;
	private final Property<Boolean> busy = new Property<Boolean>(false);

	public Property<Boolean> busyProperty() {
		return busy;
	}
	
	public Topics(final Connection connection) throws SQLException {
		this._getFromForum = connection.prepareStatement("select last_msg_id \n" +
				"from topics \n" +
				"where forum_id=? \n" +
				"order by last_msg_id desc");

		this._readTopic = connection.prepareStatement("select m_topic.*, u_topic.name, m_last.*, u_last.name from messages m_last \n" +
				"left join users u_last on u_last.user_id=m_last.user_id \n" +
				"left join messages m_topic on m_topic.message_id=m_last.topic_id \n" +
				"left join users u_topic on u_topic.user_id=m_topic.user_id \n" +
				"where m_last.message_id=?");
	}
	
	public Iterable<Long> getFromForum(final long forumId) {
		busy.set(true);
		try {
			_getFromForum.setLong(1, forumId);
			final ResultSet rows = _getFromForum.executeQuery();
			return new LongEntityController.LazyCollectionLongWrapper(rows);
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	public final Message[] readTopic(final long lastMsgId) {
		try {
			_readTopic.setLong(1, lastMsgId);
			final ResultSet rows = _readTopic.executeQuery();
			rows.next();
			final MessageBuilder mb = MessageBuilder.create();
			return  new Message[]{ Messages.readRow(rows, 0, mb).build(), Messages.readRow(rows, 11, mb).build() };
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}	
	
}
