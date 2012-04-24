package com.agorikov.rsdnhome.app;

import android.content.res.Resources;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.ComposedMessage;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.ComposedMessage.ComposedMessageBuilder;

public abstract class MessageComposeUtils {
	final static String TAG = "MessageComposeUtils";

	/**
	 * Compose both reply and new message with this method.
	 * For new message, set parentId to 0 and give real forumId.
	 * For reply, give real message id and ignore forumId. 
	 * @param subj
	 * @param body
	 * @param parentId set to 0 for new message
	 * @param forumId  ignored when parentId is not 0
	 * @return
	 */
	public static ComposedMessage composeMessage(
			final String subj, final String body,
			final long parentId, final long forumId) {
		Log.d(TAG, subj);
		Log.d(TAG, body);
		final RSDNApplication app = RSDNApplication.getInstance();
		final Message parent = app.getMessages().get(parentId);
		final Resources r = RSDNApplication.getInstance().getResources();
		final String tagLine = "[tagline]" + String.format(r.getString(R.string.tagline), r.getString(R.string.app_name)) + "[/tagline]";
		
		return ComposedMessageBuilder.create()
				.id(0).parentId(parentId).forumId(parent != null ? parent.getForumId() : forumId)
				.subj(subj.trim())
				.body(body.trim() + "\r\n\r\n" + tagLine)
				.build();
	}
}
