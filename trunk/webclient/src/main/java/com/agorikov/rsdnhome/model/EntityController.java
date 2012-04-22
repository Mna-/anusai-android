package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.agorikov.rsdnhome.beans.ObservableValue;
import com.agorikov.rsdnhome.common.Strings;
import com.agorikov.rsdnhome.common.util.Log;

public abstract class EntityController<E> {
	static final String TAG = "EntityController";

	public abstract ObservableValue<Boolean> busy();
	
	public abstract Iterable<E> getAll();
	public abstract E get(long id);

	public static final int SINGLE_BATCH = 1;
	public static final int SMALL_BATCH = 4;
	public static final int MEDIUM_BATCH = 11;
	public static final int LARGE_BATCH = 51;
	
	private final PreparedStatement _removeAll;
	//private final PreparedStatement _removeAll2;
	private final PreparedStatement _removeAll3;
	private final PreparedStatement _selectIds[];
	
	protected EntityController(final Connection connection, final String table, final String pkey) throws SQLException {
		this._removeAll = connection.prepareStatement(String.format("delete from %s", table));
		//this._removeAll2 = connection.prepareStatement(String.format("delete from %s where %s in (?)", table, pkey));
		this._removeAll3 = connection.prepareStatement(String.format("delete from %s where %s=?", table, pkey));
		this._selectIds = new PreparedStatement[] { 
			connection.prepareStatement(String.format("select %s from %s where %s in (", pkey, table, pkey) + Strings.joinPattern(",", "?", SINGLE_BATCH) + ")"), 
			connection.prepareStatement(String.format("select %s from %s where %s in (", pkey, table, pkey) + Strings.joinPattern(",", "?", SMALL_BATCH) + ")"),
			connection.prepareStatement(String.format("select %s from %s where %s in (", pkey, table, pkey) + Strings.joinPattern(",", "?", MEDIUM_BATCH) + ")"),
			connection.prepareStatement(String.format("select %s from %s where %s in (", pkey, table, pkey) + Strings.joinPattern(",", "?", LARGE_BATCH) + ")") 
		};
	}
	
	public abstract void putAll(final List<E> items);

	public final void putAll(final E ...items) {
		putAll(Arrays.asList(items));
	}
	
	protected final Set<Long> intersectIds(final Collection<? extends Entity> items) throws SQLException {
		final Set<Long> existingIds = new HashSet<Long>();
		int numberLeft = items.size();
		final Iterator<? extends Entity> it = items.iterator();
		while (numberLeft > 0) {
			if (numberLeft >= LARGE_BATCH) {
				executeBatch(LARGE_BATCH, _selectIds[3], existingIds, it);
				numberLeft -= LARGE_BATCH;
			} else if (numberLeft >= MEDIUM_BATCH) {
				executeBatch(MEDIUM_BATCH, _selectIds[2], existingIds, it);
				numberLeft -= MEDIUM_BATCH;
			} else if (numberLeft >= SMALL_BATCH) {
				executeBatch(SMALL_BATCH, _selectIds[1], existingIds, it);
				numberLeft -= SMALL_BATCH;
			} else {
				executeBatch(SINGLE_BATCH, _selectIds[0], existingIds, it);
				numberLeft -= SINGLE_BATCH;
			}
		}
		return existingIds;
	}

	private static void executeBatch(final int batchSize, final PreparedStatement selectIds, final Set<Long> existingIds,
			final Iterator<? extends Entity> it) throws SQLException {
		for (int i = 0; i != batchSize; ++i) {
			final long id = it.next().getId();
			selectIds.setLong(1 + i, id);
		}
		final ResultSet results = selectIds.executeQuery();
		while (results.next()) {
			existingIds.add(results.getLong(1));
		}
	}

	public final void removeAll(long... ids) {
		try {
			if (ids.length != 0) {
				for (final long id : ids) {
					_removeAll3.setLong(1, id);
					_removeAll3.executeUpdate();
				}
			} else {
				_removeAll.executeUpdate();
			}
		} catch (SQLException e) {
			Log.e(TAG, "Error in removeAll", e);
		}
	}
	
	protected static java.sql.Timestamp date(final java.util.Date d) {
		return d != null ? new java.sql.Timestamp(d.getTime()) : null;
	}
}
