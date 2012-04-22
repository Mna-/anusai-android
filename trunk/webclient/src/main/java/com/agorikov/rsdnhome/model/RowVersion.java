package com.agorikov.rsdnhome.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.persist.DataModel;

public enum RowVersion implements RowVersionProvider {
	Forums, Rating, Message, Moderate, Users;

	/**
	 * 64bit value 0;
	 */
	public final static String INITIAL_BASE64_VERSION = "AAAAAAAAAAA=";
	static final String TAG = "RowVersion";
	
	private static class Holder {
		final static PreparedStatement queryStatement;
		final static PreparedStatement updateStatement;
		static {
			final DataModel model = new DataModel();
			try {
				queryStatement = model.connection().prepareStatement("select base64 from rowversion where rowversion_id = ?");
				updateStatement = model.connection().prepareStatement("merge into rowversion values (?,?)");
			} catch (SQLException e) {
				Log.e(TAG, "Error in prepareStatement", e);
				throw new RuntimeException(e);
			}
		}
	}

	public static final Map<String, RowVersion> responseNames;
	
	static {
		Map<String, RowVersion> names = new HashMap<String, RowVersion>();
		for (final RowVersion ver : RowVersion.values()) {
			names.put(ver.getResponseName(), ver);
		}
		responseNames = Collections.unmodifiableMap(names);
	}
	
	@Override
	public String get() {
		String base64 = INITIAL_BASE64_VERSION;
		try {
			Holder.queryStatement.setLong(1, ordinal());
			final ResultSet resultSet = Holder.queryStatement.executeQuery();
			if (resultSet.next())
				base64 = resultSet.getString(1);
		} catch (SQLException e) {
			Log.e(TAG, "Error in get", e);
		}
		return base64;
	}
	
	@Override
	public void put(final String base64) {
		try {
			Holder.updateStatement.setLong(1, ordinal());
			Holder.updateStatement.setString(2, base64);
			Holder.updateStatement.executeUpdate();
		} catch (final SQLException e) {
			Log.e(TAG, "Error in put", e);
		}
	}
	
	@Override
	public String getRequestName() {
		switch (this) {
		case Users:
			return "lastRowVersion";
		default:
			final String name = name();
			return name.substring(0, 1).toLowerCase() + name.substring(1) + "RowVersion";
		}
	}
	
	@Override
	public String getResponseName() {
		switch (this) {
		case Users:
		case Forums:
			return getRequestName();
		case Message:
			return "lastForumRowVersion";
		default:
			final String name = name();
			return "last" + name.substring(0, 1).toUpperCase() + name.substring(1) + "RowVersion";
		}
	}
}
