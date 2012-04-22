package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.agorikov.rsdnhome.beans.ObservableValue;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.model.ForumGroup.ForumGroupBuilder;

public final class ForumGroups extends EntityController<ForumGroup> {
	
	private final Connection connection;
	private final PreparedStatement _getAll;
	private final PreparedStatement _get;
	private final PreparedStatement _putAll;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	
	public ForumGroups(final Connection connection) throws SQLException {
		super(connection, "forum_groups", "forum_group_id");
		this.connection = connection;
		this._getAll = this.connection.prepareStatement("select * from forum_groups");
		this._get = this.connection.prepareStatement("select * from forum_groups where forum_group_id=?");
		this._putAll = this.connection.prepareStatement("merge into forum_groups values(?,?)" );
	}

	public static final Comparator<ForumGroup> compareByName = new Comparator<ForumGroup>() {
		@Override
		public int compare(ForumGroup group1, ForumGroup group2) {
			return group1.getName().compareTo(group2.getName());
		}};
	
	@Override
	public ForumGroup get(final long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final List<ForumGroup> ls = readData(resultSet);
			return !ls.isEmpty() ? ls.get(0) : null;
		} catch(SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	};	
		
	@Override
	public List<ForumGroup> getAll() {
		busy.set(true);
		try {
			final ResultSet resultSet = _getAll.executeQuery();
			return readData(resultSet);
		} catch(SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	private List<ForumGroup> readData(final ResultSet resultSet)
			throws SQLException {
		final List<ForumGroup> all = new ArrayList<ForumGroup>();
		final ForumGroupBuilder builder = ForumGroupBuilder.create();
		while (resultSet.next()) {
			builder.id(resultSet.getLong(1));
			builder.name(resultSet.getString(2));
			all.add(builder.build());
		}
		
		return Collections.unmodifiableList(all);
	}

	@Override
	public void putAll(List<ForumGroup> items) {
		busy.set(true);
		try {
			for (final ForumGroup group : items) {
				_putAll.setLong(1, group.getId());
				_putAll.setString(2, group.getName());
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
		return this.busy();
	}
	
	
}
