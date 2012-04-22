package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.agorikov.rsdnhome.beans.ObservableValue;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.LazyCollection;
import com.agorikov.rsdnhome.model.LongEntityController.LazyCollectionLongWrapper;
import com.agorikov.rsdnhome.model.Message.MessageBuilder;

public final class Messages extends EntityController<Message> {

	private final PreparedStatement _getAll;
	private final PreparedStatement _get;
	private final PreparedStatement _getFromForum;
	private final PreparedStatement _getFromParent;
	private final PreparedStatement _putAll, _putAllUpdateTopic;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	
	public Messages(final Connection connection) throws SQLException {
		super(connection, "messages", "message_id");
		this._getAll = connection.prepareStatement("select m.*, u.name from messages m left join users u on u.user_id=m.user_id");
		this._get = connection.prepareStatement("select m.*, u.name from messages m left join users u on u.user_id=m.user_id where m.message_id=?");

		this._getFromForum = connection.prepareStatement(
				"select m.*, u.name from messages m left join users u on u.user_id=m.user_id where m.forum_id=?"
				);

		this._getFromParent = connection.prepareStatement(
				"select m.message_id from messages m where m.parent_id=? order by m.message_id asc"
				);
		
		this._putAll = connection.prepareStatement("merge into messages values(?,?,?,?,?,?,?,?,?,?)");
		this._putAllUpdateTopic = connection.prepareStatement("merge into topics (topic_id, last_msg_id, forum_id) \n" +
				"(select m.topic_id, \n" +
				"(case when t.last_msg_id > m.message_id then t.last_msg_id else m.message_id end) last_msg_id, \n" +
				"m.forum_id from messages m left join topics t on t.topic_id=m.topic_id where m.message_id=?)");
	}

	@Override
	public Message get(long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final Iterator<Message> ls = readData(resultSet, 0).iterator();
			return ls.hasNext() ? ls.next() : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	@Override
	public Iterable<Message> getAll() {
		busy.set(true);
		try {
			ResultSet resultSet;
			resultSet = _getAll.executeQuery();
			return readData(resultSet, 0);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	public Iterable<Message> getFromForum(final long forumId) {
		busy.set(true);
		try {
			_getFromForum.setLong(1, forumId);
			final ResultSet resultSet = _getFromForum.executeQuery();
			return readData(resultSet, 0);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	public Iterable<Long> getFromParent(final long messageId) {
		busy.set(true);
		try {
			_getFromParent.setLong(1, messageId);
			final ResultSet resultSet = _getFromParent.executeQuery();
			return new LazyCollectionLongWrapper(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	private Iterable<Message> readData(final ResultSet resultSet, final int offset) throws SQLException {
		final MessageBuilder builder = MessageBuilder.create();
		return new LazyCollection<Message>(resultSet) {
			@Override
			public Message next() {
				final Message m;
				try {
					m = readRow(resultSet, offset, builder).build();
					doNext();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
				return m;
			}
		};
	}

	static MessageBuilder readRow(final ResultSet resultSet,
			final MessageBuilder builder) throws SQLException {
		return readRow(resultSet, 0, builder);
	}
	
	static MessageBuilder readRow(final ResultSet resultSet, final int offset,
			final MessageBuilder builder) throws SQLException {
		return builder
				.id(resultSet.getLong(offset + 1))
				.topicId(resultSet.getLong(offset + 2))
				.parentId(resultSet.getLong(offset + 3))
				.userId(resultSet.getLong(offset + 4))
				.forumId(resultSet.getLong(offset + 5))
				.subject(resultSet.getString(offset + 6))
				.body(resultSet.getString(offset + 7))
				.articleId(resultSet.getLong(offset + 8))
				.messageDate(resultSet.getTimestamp(offset + 9))
				.lastModerated(resultSet.getTimestamp(offset + 10))
				.userName(resultSet.getString(offset + 11));
	}

	@Override
	public void putAll(final List<Message> items) {
		busy.set(true);
		try {
			for (final Message message : items) {
				_putAll.setLong(1, message.getId());
				_putAll.setLong(2, message.getTopicId());
				_putAll.setObject(3, message.getParentId());
				_putAll.setObject(4, message.getUserId());
				_putAll.setLong(5, message.getForumId());
				_putAll.setString(6, message.getSubject());
				_putAll.setString(7, message.getBody());
				_putAll.setObject(8, message.getArticleId());
				_putAll.setTimestamp(9, date(message.getMessageDate()));
				_putAll.setTimestamp(10, date(message.getLastModerated()));
				_putAllUpdateTopic.setLong(1, message.getId());
				
				_putAll.executeUpdate();
				_putAllUpdateTopic.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	@Override
	public final ObservableValue<Boolean> busy() {
		return this.busy;
	}	
}
