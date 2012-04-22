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
import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.ComposedMessage.ComposedMessageBuilder;

public final class ComposedMessages extends EntityController<ComposedMessage> {

	private final PreparedStatement _getAll;
	private final PreparedStatement _get;
	private final PreparedStatement _putAll;
	private final PreparedStatement _deleteByIds;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	
	
	
	public ComposedMessages(final Connection connection) throws SQLException {
		super(connection, "composed_messages", "composed_message_id");
		this._getAll = connection.prepareStatement("select * from composed_messages");
		this._get = connection.prepareStatement("select * from composed_messages where composed_message_id=?");

		this._putAll = connection.prepareStatement("insert into composed_messages (parent_message_id, forum_id, subject, body) values(?,?,?,?)");
		this._deleteByIds = connection.prepareStatement("delete from composed_messages where composed_message_id in (select id from TABLE(id long=?) t)");
	}

	@Override
	public ObservableValue<Boolean> busy() {
		return busy;
	}

	@Override
	public Iterable<ComposedMessage> getAll() {
		busy.set(true);
		try {
			ResultSet resultSet;
			resultSet = _getAll.executeQuery();
			return readData(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	@Override
	public ComposedMessage get(long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final Iterator<ComposedMessage> ls = readData(resultSet).iterator();
			return ls.hasNext() ? ls.next() : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	@Override
	public void putAll(final List<ComposedMessage> items) {
		busy.set(true);
		try {
			for (final ComposedMessage message : items) {
				_putAll.setLong(1, message.getParentId());
				_putAll.setLong(2, message.getForumId());
				_putAll.setString(3, message.getSubj());
				_putAll.setString(4, message.getBody());
				_putAll.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	private Iterable<ComposedMessage> readData(final ResultSet resultSet) throws SQLException {
		final ComposedMessageBuilder builder = ComposedMessageBuilder.create();
		return new LazyCollection<ComposedMessage>(resultSet) {
			@Override
			public ComposedMessage next() {
				final ComposedMessage m;
				try {
					m = readRow(resultSet, builder).build();
					doNext();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
				return m;
			}
		};
	}
	
	static ComposedMessageBuilder readRow(final ResultSet resultSet, 
			final ComposedMessageBuilder builder) throws SQLException {
		return builder
				.id(resultSet.getLong(1))
				.parentId(resultSet.getLong(2))
				.forumId(resultSet.getLong(3))
				.subj(resultSet.getString(4))
				.body(resultSet.getString(5));
	}

	public void deleteByIds(final List<Long> sentIds) {
		if (!sentIds.isEmpty()) {
			try {
				_deleteByIds.setObject(1, sentIds.toArray());
				_deleteByIds.executeUpdate();
			} catch (SQLException e) {
				Log.e(TAG, "Error in deleteByIds", e);
				throw new RuntimeException(e);
			}
		}
		
		
	}
	
	
}
