package com.agorikov.rsdnhome.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.kobjects.base64.Base64;

import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.util.Log;

public final class ForumRowVersions {
	private final PreparedStatement _get, _get2, _put, _selectedForumCount, _brokenTopicIds;
	private final Property<Boolean> busy = new Property<Boolean>(false);
	private final RowVersion version;

	public ForumRowVersions(final Connection connection, final RowVersion version) {
		this.version = version;
		try {
			this._get = connection.prepareStatement("select GROUP_CONCAT(f.forum_id) as forum_ids, f.rowversion from \n" +
					"(select f.forum_id, case when v.rowversion is null then 0 else v.rowversion end as rowversion \n" +
					"from selected_forums f \n" +
					"left join forum_rowversion v on v.forum_rowversion_id=BITOR(f.forum_id, ? * 0x100000000000004) \n" +
					") f \n" +
					"group by f.rowversion \n" +
					"order by f.rowversion desc \n");
			this._get2 = connection.prepareStatement("select f.rowversion from forum_rowversion f \n" +
					"where f.forum_rowversion_id=BITOR(?, ? * 0x100000000000004) ");
			this._selectedForumCount = connection.prepareStatement("select count(forum_id) from selected_forums");
			this._put = connection.prepareStatement("merge into forum_rowversion values (BITOR(?, ? * 0x100000000000004),?)");

			this._brokenTopicIds = connection.prepareStatement("select distinct m1.topic_id from \n" +
					"selected_forums f \n" +
					"inner join messages m1 on m1.forum_id=f.forum_id \n" +
					"left join messages m2 on m2.message_id=m1.parent_id \n" +
					"where m1.topic_id <> m1.message_id and m2.message_id is null \n");
		
		
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static abstract class ForumRowVersion implements RowVersionProvider {
		public abstract List<Long> getForumIds();
		public abstract boolean incomplete();
		public abstract long getRaw();
	}
	
	public ForumRowVersion getTopForumRowVersion(final Set<Long> lastRowVersionSkip) {
		busy.set(true);
		try {
			final ResultSet selectedCountRS = _selectedForumCount.executeQuery();
			selectedCountRS.next();
			final long selectedCount = selectedCountRS.getLong(1);
			_get.setLong(1, version.ordinal());
			final ResultSet rs = _get.executeQuery();
			return readData(lastRowVersionSkip, selectedCount, rs);
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		} finally {
			busy.set(false);
		}
	}

	public ForumRowVersion getConnectedObject(final ForumRowVersion master, final RowVersion version) {
		long rawversion = 0;
		final List<Long> forumIds = master.getForumIds();
		if (!forumIds.isEmpty()) {
			try {
				final Long forumId = forumIds.get(0);
				_get2.setLong(1, forumId);
				_get2.setLong(2, version.ordinal());
				
				final ResultSet rs = _get2.executeQuery();
				if (rs.next())
					rawversion = rs.getLong(1);
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
		}
		return createForumRowVersionObject(version, rawversion, forumIds, false);
	}
	
	private void putForumIdRowVersion(final RowVersion version, final long forumId, final long rowVersion) throws SQLException {
		_put.setLong(1, forumId);
		_put.setLong(2, version.ordinal());
		_put.setLong(3, rowVersion);
		_put.executeUpdate();
	}

	private static long decodeBase64(final String rowVersion) {
		final byte[] rawBytes = Base64.decode(rowVersion);
		if (rawBytes.length != 8)
			throw new RuntimeException(String.format("Corrupted rowVersion {%s}", rowVersion));
		long rowVersionInt = 0;
		for (int i = 0; i < rawBytes.length; ++i)
			rowVersionInt = (rowVersionInt << 8) | rawBytes[i] & 0xff;
		return rowVersionInt;
	}

	private static String encodeBase64(long rowVersion) {
		byte[] rawBytes = new byte[8];
		for (int i = rawBytes.length - 1; i >= 0; --i) {
			rawBytes[i] = (byte) (rowVersion & 0xff);
			rowVersion = rowVersion >>> 8;
		}
		
		return Base64.encode(rawBytes);
	}
	
	private ForumRowVersion readData(final Set<Long> lastRowVersionSkip, long selectedCount, final ResultSet rs) throws SQLException {
		while (selectedCount > 0 && rs.next()) {
			final long rowVersion = rs.getLong(2);
			if (lastRowVersionSkip.contains(rowVersion)) {
				--selectedCount;
				continue;
			}
			final ArrayList<Long> forumIds = new ArrayList<Long>();
			final String sforumIds = rs.getString(1);
			for (final String sforumId : sforumIds.split(",")) {
				forumIds.add(Long.parseLong(sforumId));
			}
			final List<Long> rdForumIds = Collections.unmodifiableList(forumIds);
			return createForumRowVersionObject(version, rowVersion, rdForumIds, rdForumIds.size() < selectedCount);
		}
		return null;
	}

	private ForumRowVersion createForumRowVersionObject(final RowVersion version, final long rowVersion, 
			final List<Long> rdForumIds, final boolean incomplete) {
		return new ForumRowVersion() {
			long rawValue;
			String base64;
			{
				if ((version == RowVersion.Moderate || version == RowVersion.Rating) && rowVersion == 0)
					rawValue = Long.MAX_VALUE;
				else
					rawValue = rowVersion;
				base64 = encodeBase64(rawValue);
			}
			@Override
			public String get() {
				return base64;
			}
			@Override
			public void put(final String base64) {
				try {
					this.rawValue = decodeBase64(base64);
					this.base64 = base64;
					for (final long forumId : rdForumIds) {
						ForumRowVersions.this.putForumIdRowVersion(version, forumId, rawValue);
					}
				} catch (final SQLException e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public String getRequestName() {
				return version.getRequestName();
			}
			@Override
			public String getResponseName() {
				return version.getResponseName();
			}

			@Override
			public List<Long> getForumIds() {
				return rdForumIds;
			}
			@Override
			public boolean incomplete() {
				return incomplete;
			}
			@Override
			public String toString() {
				return ":" + version;
			}
			@Override
			public long getRaw() {
				return rawValue;
			}
		};
	}

	public Iterable<Long> getBreakTopicIds() {
		try {
			final Iterable<Long> topicIds = new LongEntityController.LazyCollectionLongWrapper(_brokenTopicIds.executeQuery());
			if (topicIds.iterator().hasNext())
				return topicIds;
			else
				return null;
		} catch (SQLException e) {
			Log.e("ForumRowVersions", "Error in getBreakTopicIds", e);
			throw new RuntimeException(e);
		}
	}
	
	
	
//	public static void main(String[] args) throws SQLException {
//		final long value = Long.MIN_VALUE;
//		final String base64 = encodeBase64(value);
//		final long decodedValue = decodeBase64(base64);
//		if (value != decodedValue)
//			throw new RuntimeException();
//		
//		final DataModel dataModel = new DataModel();
//		final Preferences preferences = new Preferences(dataModel.connection());
//		preferences.selectForum(30, true);
//		preferences.selectForum(34, true);
//		
//		final ForumRowVersions forumRowVersions = new ForumRowVersions(dataModel.connection(), RowVersion.Message);
//		final ForumRowVersion messageVersion = forumRowVersions.getTopForumRowVersion();
//		final ForumRowVersion ratingVersion = forumRowVersions.getConnectedObject(messageVersion, RowVersion.Rating);
//		
//		messageVersion.put(base64);
//		final String string = forumRowVersions.getTopForumRowVersion().get();
//		
//		ratingVersion.put(base64);
//		final String string2 = forumRowVersions.getConnectedObject(messageVersion, RowVersion.Rating).get();
//		
//	}
	
	
}
