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

public final class LongEntityController extends EntityController<Long> {
	private final PreparedStatement _get;
	private final PreparedStatement _getAll;
	private final PreparedStatement _putAll;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	
	public LongEntityController(final Connection connection, final String table, final String pkey) throws SQLException {
		super(connection, table, pkey);
		this._getAll = connection.prepareStatement(String.format("select * from %s", table));
		this._get = connection.prepareStatement(String.format("select * from %s where %s=?", table, pkey));
		this._putAll = connection.prepareStatement(String.format("merge into %s values(?)", table));
	}

	@Override
	public Long get(long id) {
		busy.set(true);
		try {
			_get.setLong(1, id);
			final ResultSet resultSet = _get.executeQuery();
			final Iterator<Long> ls = new LazyCollectionLongWrapper(resultSet).iterator();
			return ls.hasNext() ? ls.next() : null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}
	
	@Override
	public Iterable<Long> getAll()  {
		busy.set(true);
		try {
			final ResultSet resultSet = _getAll.executeQuery();
			return new LazyCollectionLongWrapper(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	public static final class LazyCollectionLongWrapper extends LazyCollection<Long> {
		public LazyCollectionLongWrapper(final ResultSet resultSet) {
			super(resultSet);
		}
		@Override
		public Long next() {
			final long result;
			try {
				result = resultSet.getLong(1);
				super.doNext();
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
			return result;
		}		
	}
		
	@Override
	public void putAll(final List<Long> items)  {
		busy.set(true);
		try {
			for (final Long id : items) {
				_putAll.setLong(1, id);
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
