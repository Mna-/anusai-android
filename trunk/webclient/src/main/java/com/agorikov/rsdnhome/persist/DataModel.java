package com.agorikov.rsdnhome.persist;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.agorikov.rsdnhome.common.util.Log;

public final class DataModel {

	private final static String JDBC_DRIVER_NAME = "org.h2.Driver";
	public final static String SETTINGS_DIR = getStorageDirectory() + File.separator + ".rsdn.home";
	private final static String DB_FILE_NAME = SETTINGS_DIR + File.separator + "storage";
	private final static String JDBC_URL = "jdbc:h2:split:" + DB_FILE_NAME + ";LOG=1";// + ";CACHE_SIZE=512;MAX_OPERATION_MEMORY=10000";
	private final static String JDBC_USER = "artem";
	private final static String JDBC_PASSWORD = "";
	
	public static String getStorageDirectory() {
		final String homeDir = System.getProperty("user.home");
		if (homeDir != null && !homeDir.equals(""))
			return homeDir;
		try {
			final Class<?> environment = Class.forName("android.os.Environment");
			final File file = (File) environment.getMethod("getExternalStorageDirectory").invoke(null);
			return file.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private static class Holder {
		final static Connection connection;
		static {
			try {
				Class.forName(JDBC_DRIVER_NAME);
				final File f = new File(DB_FILE_NAME + ".h2.db");
				
				//f.delete();
				
				final boolean needToInitialize = !f.exists();
				if (needToInitialize)
					new File(SETTINGS_DIR).mkdirs();
				connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
				initializeDatabase();
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (final SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	};
	
	private static void initializeDatabase() throws SQLException {
		final Statement statement = Holder.connection.createStatement();
		statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
		//statement.execute("SET UNDO_LOG 0");
		//statement.execute("SET THROTTLE 50");
		//statement.execute("SET TRACE_LEVEL_FILE 0");
		//statement.execute("SET MULTI_THREADED 1");
		
		statement.execute("create table if not exists rowversion (" +
				"rowversion_id serial not null," +
				" base64 text," +
				" primary key (rowversion_id)" +
				")");
		statement.execute("create table if not exists forum_rowversion (" +
				"forum_rowversion_id serial not null," +
				" rowversion bigint not null," +
				" primary key (forum_rowversion_id)" +
				")");
		statement.execute("create table if not exists users (" +
				"user_id serial not null," +
				" name text not null," +
				" real_name text," +
				" email text," +
				" www text," +
				" specialization text," +
				" where_from text," +
				" origin text," +
				" role long not null," +
				" primary key (user_id)" +
				")");
		statement.execute("create table if not exists forum_groups (" +
				"forum_group_id serial not null," +
				" name text not null," +
				" primary key (forum_group_id)" +
				")");
		statement.execute("create table if not exists forums (" +
				"forum_id serial not null," +
				" forum_group_id long," +
				" short_name text not null," +
				" full_name text not null," +
				" primary key (forum_id)," +
				" foreign key (forum_group_id) references forum_groups(forum_group_id)" +
				")");
		statement.execute("create table if not exists messages (" +
				"message_id serial not null," +
				" topic_id long not null," +
				" parent_id long," +
				" user_id long," +
				" forum_id long not null," +
				" subject text," +
				" body text," +
				" article_id long," +
				" message_date timestamp not null," +
				" last_moderated timestamp," +
				" primary key (message_id)," +
				" foreign key (topic_id) references messages(message_id)," +
				" foreign key (parent_id) references messages(message_id)," +
				" foreign key (user_id) references users(user_id)," +
				" foreign key (forum_id) references forums(forum_id)" +
				")");
		statement.execute("create table if not exists topics (" +
				"topic_id serial not null," +
				" last_msg_id long not null," +
				" forum_id long not null," +
				" primary key (topic_id)," +
				" foreign key (last_msg_id) references messages(message_id)," +
				" foreign key (forum_id) references forums(forum_id)" +
				")");
		{
			final ResultSet rs = statement.executeQuery("select (count(*) = 0) as empty from topics");
			rs.next();
			final boolean isEmpty = rs.getBoolean(1);
			if (isEmpty) {
				statement.execute("merge into topics (topic_id, last_msg_id, forum_id) \n" +
						"(select m.*, m2.forum_id from \n" +
						"  (select topic_id, max(message_id) last_msg_id from messages group by topic_id) m \n" +
						"  left join messages m2 on m2.message_id=m.last_msg_id)");
			}
		}
		//statement.execute("update messages set topic_id=message_id where topic_id=0");
		statement.execute("create table if not exists selected_forums (" +
				"forum_id serial not null," +
				" primary key (forum_id)" +
				")");
		
		statement.execute("create table if not exists composed_messages (\n" +
				"composed_message_id serial not null," +
				"parent_message_id long," +
				"forum_id long not null," +
				"subject text," +
				"body text," +
				"primary key (composed_message_id)," +
				"foreign key (parent_message_id) references messages(message_id)," +
				"foreign key (forum_id) references forums(forum_id)" +
				")");
		
	}
	
	public Connection connection() {
		return Holder.connection;
	}
	
	public void flush() {
		try {
			connection().createStatement().execute("CHECKPOINT SYNC");
		} catch (SQLException e) {
			Log.e("DataModel", "flush", e);
		}
	}
	
	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {

		
		final DataModel model = new DataModel();
		model.connection();
//		final Forums forums = new Forums(model.connection());
//		final Users users = new Users(model.connection());
//		final Messages messages = new Messages(model.connection());

//		System.out.println(String.format("Start generate all forums @ %s", new Date()));
//		final String forumName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"v" +
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
//				"v";
//
//		final ForumBuilder forumBuilder = ForumBuilder.create();
//		for (long i = 0; i != Long.MAX_VALUE; ++i) {
//			forumBuilder.id(i + 1).groupId(null).fullName(forumName).shortName(forumName);
//			forums.putAll(forumBuilder.build());
//		}
//		System.out.println(String.format("All forums generated @ %s", new Date()));
		
//		for (final Forum forum : forums.getAll()) {
//			System.out.println(forum);
//		}
//		
//		for (final User user : users.getAll()) {
//			System.out.println(user);
//		}
//		

//		for (final Message message : messages.getFromForum(30)) {
//			System.out.println(message);
//		}
		
		
		//final Preferences preferences = new Preferences(model.connection());
		//preferences.selectForum(30, false);
		
		//System.out.println(forums.getAvailable());
		
		// Get topic list
//		final Topics topics = new Topics(model.connection());
//		System.out.println(String.format("=======START TOPICS========%s", new Date()));
//		final Iterable<Topic> topicList = topics.getFromForum(30, 0, 10);
//		System.out.println("topicList");
//		for (final Topic topic : topicList) {
//			System.out.println(topic);
//		}
//		System.out.println("EO topicList");
//		System.out.println(String.format("=======TOPICS FINISHED OK========%s", new Date()));
	}

}
