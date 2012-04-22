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
import com.agorikov.rsdnhome.model.User.UserBuilder;

public final class Users extends EntityController<User> {

	private final PreparedStatement _getAll;
	private final PreparedStatement _get;
	private final PreparedStatement _putAll;
	private final Property<Boolean> busy = new Property<Boolean>(false);

	public Users(final Connection connection) throws SQLException {
		super(connection, "users", "user_id");
		this._getAll = connection.prepareStatement("select * from users");
		this._get = connection.prepareStatement("select * from users where user_id=?");
		this._putAll = connection.prepareStatement("merge into users values(?,?,?,?,?,?,?,?,?)");
	}

	@Override
	public User get(long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final Iterator<User> ls = readData(resultSet).iterator();
			return ls.hasNext() ? ls.next() : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	@Override
	public Iterable<User> getAll() {
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


	private Iterable<User> readData(final ResultSet resultSet) throws SQLException {
		final UserBuilder builder = UserBuilder.create();
		return new LazyCollection<User>(resultSet) {
			@Override
			public User next() {
				final User user;
				try {
					user = builder
						.id(resultSet.getLong(1))
						.name(resultSet.getString(2))
						.realName(resultSet.getString(3))
						.email(resultSet.getString(4))
						.www(resultSet.getString(5))
						.specialization(resultSet.getString(6))
						.whereFrom(resultSet.getString(7))
						.origin(resultSet.getString(8))
						.role((Long) resultSet.getObject(9))
					.build();
					doNext();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
				return user;
			}
			
		};
	}

	@Override
	public void putAll(final List<User> items) {
		busy.set(true);
		try {
			for (final User user : items) {
				_putAll.setLong(1, user.getId());
				_putAll.setString(2, user.getName());
				_putAll.setString(3, user.getRealName());
				_putAll.setString(4, user.getEmail());
				_putAll.setString(5, user.getWww());
				_putAll.setString(6, user.getSpecialization());
				_putAll.setString(7, user.getWhereFrom());
				_putAll.setString(8, user.getOrigin());
				_putAll.setObject(9, user.getRole());
				
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
	
	
}
