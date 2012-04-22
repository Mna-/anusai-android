package com.agorikov.rsdnhome.webclient;

import java.sql.SQLException;
import java.util.LinkedList;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.common.util.Log.UtilLog;
import com.agorikov.rsdnhome.model.Credentials.CredentialsBuilder;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.Forums;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.MessageEntity;
import com.agorikov.rsdnhome.model.Messages;
import com.agorikov.rsdnhome.model.Preferences;
import com.agorikov.rsdnhome.persist.DataModel;
import com.agorikov.rsdnhome.webclient.model.RsdnWebService;
import com.agorikov.rsdnhome.webclient.model.SimpleWebMethod.EntityReceiver;

public class KSoapClient {
	static final String TAG = "KSoapClient";
	
	static final String SOAP_ACTION_NS = "http://rsdn.ru/Janus/";
	static final String WS_URL = "http://rsdn.ru/WS/JanusAT.asmx";
	static final int CACHED_ENTITIES_SIZE = 1000;
	
	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {
		Log.registerUtilLog(new Log.DefaultUtilLog());
		
		final DataModel dataModel = new DataModel();
		final Forums forums = new Forums(dataModel.connection());
		//final ForumGroups forumGroups = new ForumGroups(dataModel.connection());
		final Messages messages = new Messages(dataModel.connection());
		//final Users users = new Users(dataModel.connection());
		final Preferences preferences = new Preferences(dataModel.connection());
		//final Topics topics = new Topics(dataModel.connection());
		
		preferences.selectForum(30, true);
		preferences.selectForum(34, false);

		final boolean updateData = true;
		
		if (updateData) {
			final RsdnWebService ws = new RsdnWebService("KSoapClient-0.0.1", dataModel.connection());
			ws.setCredentials(CredentialsBuilder.create().userName("").password("").build());
			
//			ws.getForumList.call(new EntityReceiver<ForumEntity>() {
//				final LinkedList<Forum> accForums = new LinkedList<Forum>();
//				final LinkedList<ForumGroup> accForumGroups = new LinkedList<ForumGroup>();
//				@Override
//				public boolean consume(final ForumEntity result) {
//					if (result instanceof Forum)
//						accForums.add((Forum) result);
//					else if (result instanceof ForumGroup)
//						accForumGroups.add((ForumGroup) result);
//					
//					if (accForums.size() >= CACHED_ENTITIES_SIZE)
//						flushForums();
//					if (accForumGroups.size() >= CACHED_ENTITIES_SIZE)
//						flushForumGroups();
//					return true;
//				}
//	
//				private void flushForumGroups() {
//					forumGroups.putAll(accForumGroups);
//					accForumGroups.clear();
//				}
//	
//				private void flushForums() {
//					forums.putAll(accForums);
//					accForums.clear();
//				}
//	
//				@Override
//				public void flush() {
//					flushForums();
//					flushForumGroups();
//				}
//			});
	
//			ws.getNewUsers.call(new EntityReceiver<UserEntity>() {
//				final LinkedList<User> accUsers = new LinkedList<User>();
//				@Override
//				public boolean consume(final UserEntity result) {
//					accUsers.add((User)result);
//					if (accUsers.size() >= CACHED_ENTITIES_SIZE)
//						flush();
//					return true;
//				}
//				@Override
//				public void flush() {
//					users.putAll(accUsers);
//					accUsers.clear();
//				}
//			});
		
			final EntityReceiver<MessageEntity> messageRecv = new EntityReceiver<MessageEntity>() {
				final LinkedList<Message> accMessages = new LinkedList<Message>();
				@Override
				public boolean consume(final MessageEntity result) {
					Log.d("Message", String.valueOf(result));
					accMessages.add((Message) result);
					if (accMessages.size() >= CACHED_ENTITIES_SIZE)
						flush();
					return true;
				}
				@Override
				public void flush() {
					messages.putAll(accMessages);
					accMessages.clear();
				}
			};
			ws.getNewData.call(messageRecv);
			ws.getBrokenTopics.call(messageRecv);
		
			Log.d(TAG, "=======FINISHED OK========");
		}


		for (final Forum forum : forums.getAll()) {
			Log.d(TAG, String.format("id:%s  Text:%s", forum.getId(), forum));
		}
				
//		System.out.println(String.format("=======START LAST MESSAGES========%s", new Date()));
//		for (final Message message : messages.getFromForum(30)) {
//			System.out.println(message);
//		}
//		System.out.println(String.format("=======LAST MESSAGES FINISHED OK========%s", new Date()));

		
//		System.out.println(String.format("=======START TOPICS========%s", new Date()));
//		final Iterable<Topic> topicList = topics.getFromForum(30);
//		for (final Topic topic : topicList) {
//			System.out.println(topic);
//		}
//		System.out.println(String.format("=======TOPICS FINISHED OK========%s", new Date()));

	}

	
}
