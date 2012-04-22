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
import com.agorikov.rsdnhome.model.Forum.ForumBuilder;

public final class Forums extends EntityController<Forum> {
	
	private final Connection connection;
	private final PreparedStatement _getAll;
	private final PreparedStatement _get;
	private final PreparedStatement _getFromGroup;
	private final PreparedStatement _getSelected;
	private final PreparedStatement _putAll;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	
	public Forums(final Connection connection) throws SQLException {
		super(connection, "forums", "forum_id");
		this.connection = connection;
		this._getAll = this.connection.prepareStatement("select * from forums");
		this._get = this.connection.prepareStatement("select * from forums where forum_id=?");
		this._getFromGroup = this.connection.prepareStatement("select * from forums where forum_group_id=?");
		this._getSelected = this.connection.prepareStatement("select f.* from forums f inner join selected_forums s_f on s_f.forum_id=f.forum_id order by f.full_name");
		this._putAll = this.connection.prepareStatement("merge into forums values(?,?,?,?)");
	}
	
	public Iterable<Forum> getFromGroup(final long forumGroupId) {
		busy.set(true);
		try {
			_getFromGroup.setLong(1, forumGroupId);
			final ResultSet resultSet = _getFromGroup.executeQuery();
			return readData(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	@Override
	public Forum get(long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final Iterator<Forum> ls = readData(resultSet).iterator();
			return ls.hasNext() ? ls.next() : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	@Override
	public Iterable<Forum> getAll() {
		busy.set(true);
		try {
			final ResultSet resultSet = _getAll.executeQuery();
			return readData(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	private Iterable<Forum> readData(final ResultSet resultSet) throws SQLException {
		final ForumBuilder builder = ForumBuilder.create();
		return new LazyCollection<Forum>(resultSet) {

			@Override
			public Forum next() {
				Forum r;
				try {
					r = builder
							.id(resultSet.getLong(1))
							.groupId(resultSet.getLong(2))
							.shortName(resultSet.getString(3))
							.fullName(resultSet.getString(4))
							.build();
				} catch (final SQLException e) {
					throw new RuntimeException(e);
				}
				doNext();
				return r;
			}
		};
	}
	
	@Override
	public void putAll(final List<Forum> items) {
		busy.set(true);
		try {
			for (final Forum forum : items) {
				_putAll.setLong(1, forum.getId());
				_putAll.setObject(2, forum.getGroupId());
				_putAll.setString(3, forum.getShortName());
				_putAll.setString(4, forum.getFullName());
				_putAll.executeUpdate();
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

	public final Iterable<Forum> getAvailable() {
		busy.set(true);
		try {
			return readData(_getSelected.executeQuery());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
}
