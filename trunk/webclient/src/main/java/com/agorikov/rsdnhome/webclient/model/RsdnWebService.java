package com.agorikov.rsdnhome.webclient.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.transport.Transport;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xmlpull.v1.XmlPullParserException;

import com.agorikov.rsdnhome.common.Converters;
import com.agorikov.rsdnhome.common.Strings;
import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.ComposedMessage;
import com.agorikov.rsdnhome.model.ComposedMessages;
import com.agorikov.rsdnhome.model.Credentials;
import com.agorikov.rsdnhome.model.Entity;
import com.agorikov.rsdnhome.model.Forum;
import com.agorikov.rsdnhome.model.Forum.ForumBuilder;
import com.agorikov.rsdnhome.model.ForumEntity;
import com.agorikov.rsdnhome.model.ForumGroup;
import com.agorikov.rsdnhome.model.ForumGroup.ForumGroupBuilder;
import com.agorikov.rsdnhome.model.ForumRowVersions;
import com.agorikov.rsdnhome.model.ForumRowVersions.ForumRowVersion;
import com.agorikov.rsdnhome.model.Message;
import com.agorikov.rsdnhome.model.Message.MessageBuilder;
import com.agorikov.rsdnhome.model.MessageEntity;
import com.agorikov.rsdnhome.model.RowVersion;
import com.agorikov.rsdnhome.model.RowVersionProvider;
import com.agorikov.rsdnhome.model.User;
import com.agorikov.rsdnhome.model.User.UserBuilder;
import com.agorikov.rsdnhome.model.UserEntity;

public final class RsdnWebService {
	static final String TAG = "RsdnWebService";
	static final String SOAP_ACTION_NS = "http://rsdn.ru/Janus/";
	static final String WS_URL = "http://rsdn.ru/WS/JanusAT.asmx";

	private Credentials credentials;
	private final ForumRowVersions messageRowVersion;
	private final Transport transport;
	private final ComposedMessages composedMessages;
	
	public final SimpleWebMethod<ForumEntity> getForumList;
	public final SimpleWebMethod<MessageEntity> getNewData;
	public final SimpleWebMethod<MessageEntity> getBrokenTopics;
	public final SimpleWebMethod<UserEntity> getNewUsers;
	public final SimpleWebMethod<Entity> postChange, postChangeCommit;
	private boolean forceFullHistory = false;
	private final Map<String, ContentHandler> contentHandlers = new HashMap<String, ContentHandler>();

	static {
		final String defaultSaxDriver = System.getProperty("org.xml.sax.driver");
		if (defaultSaxDriver == null || defaultSaxDriver.length() == 0) {
			try {
				Class.forName("org.xmlpull.v1.sax2.Driver");
				System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
			} catch (ClassNotFoundException e) {
			}
		}
	}
	
	public void setForceFullHistory(boolean enable) {
		this.forceFullHistory = enable;
	}
	
	public RsdnWebService(final String userAgent, final Connection connection) {
		this.messageRowVersion = new ForumRowVersions(connection, RowVersion.Message);
		try {
			this.composedMessages = new ComposedMessages(connection);
		} catch (SQLException e1) {
			Log.e(TAG, "Error in constructor", e1);
			throw new RuntimeException(e1);
		}
		this.transport = new HttpTransportSE(WS_URL) {
			String cookie;
			{ this.timeout = 60000; }
			
			HttpURLConnection openConnection() throws IOException {
				final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				
				connection.setReadTimeout(timeout);
				connection.setConnectTimeout(timeout);
				connection.setDoOutput(true);
				connection.setDoInput(true);
				
				return connection;
			}
			
		    @SuppressWarnings("rawtypes")
			public List call(String soapAction, final SoapEnvelope envelope, final List headers) 
		    		throws IOException, XmlPullParserException {

		    		if (soapAction == null)
		    			soapAction = "\"\"";

		    		byte[] requestData = createRequestData(envelope);
//		    		{
//		    			final File f = new File(DataModel.SETTINGS_DIR + File.separator + "request.xml");
//		    			if (f.exists())
//		    				f.delete();
//		    			final FileOutputStream file = new FileOutputStream(f);
//		    			file.write(requestData);
//		    			file.flush();
//		    			file.close();
//		    		}
		    	    
		    		requestDump = debug ? new String(requestData) : null;
		    	    responseDump = null;
		    	    
		    	    final HttpURLConnection connection = openConnection();
		    	    
		    	    connection.setRequestProperty("User-Agent", userAgent);
		    	    // SOAPAction is not a valid header for VER12 so do not add
		    	    // it
		    	    // @see "http://code.google.com/p/ksoap2-android/issues/detail?id=67
		    	    if (envelope.version != SoapSerializationEnvelope.VER12) {
		                connection.setRequestProperty("SOAPAction", soapAction);
		            }

		            if (envelope.version == SoapSerializationEnvelope.VER12) {
		                connection.setRequestProperty("Content-Type", CONTENT_TYPE_SOAP_XML_CHARSET_UTF_8);
		            } else {
		                connection.setRequestProperty("Content-Type", CONTENT_TYPE_XML_CHARSET_UTF_8);
		            }

		    	    connection.setRequestProperty("Connection", "close");
		    	    connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
		    	    connection.setRequestProperty("Content-Length", "" + requestData.length);
		    	    if (cookie != null && !cookie.equals("")) {
		    	    	connection.setRequestProperty("Cookie", cookie);
		    	    }
		    	    
		    	    // Pass the headers provided by the user along with the call
		    	    if (headers != null) {
		    		    for (int i = 0; i < headers.size(); i++) {
		    		    	HeaderProperty hp = (HeaderProperty) headers.get(i);
		    		    	connection.setRequestProperty(hp.getKey(), hp.getValue());
		    		    }
		    	    }
		    	    
		    	    connection.setRequestMethod("POST");
		    	    connection.connect();
		        

		    	    OutputStream os = connection.getOutputStream();
		       
		    	    os.write(requestData, 0, requestData.length);
		    	    os.flush();
		    	    os.close();
		    	    requestData = null;
		    	    InputStream is;
		    	    Map<String, List<String>> retHeaders = null;
		    	    
		    	    try {
		    	    	connection.connect();
		    	    	is = connection.getInputStream();
		    		    retHeaders = connection.getHeaderFields();
		    		    if (Arrays.asList("gzip").equals(retHeaders.get("Content-Encoding"))) {
		    		    	is = new GZIPInputStream(is, 256 * 1024);
		    		    }
		    	    } catch (IOException e) {
		    	    	is = connection.getErrorStream();

		    	    	if (is == null) {
		    	    		connection.disconnect();
		    	    		throw (e);
		    	    	}
		    	    }
		    	    final List<String> cookieList = retHeaders != null ? retHeaders.get("Set-Cookie") : null;
		    	    if (cookieList != null) {
		    	    	cookie = Strings.join("; ", cookieList);
		    	    }
		    	    //System.out.println(retHeaders);
		        
		    	    try {
		    	    	final String[] soapActionChunks = soapAction.split("/");
						RsdnWebService.this.readResponse(soapActionChunks[soapActionChunks.length - 1], is);
		    	    } catch (SAXException e) {
		    	    	throw new IOException(e);
					} finally {
		    	    	is.close();
		    	    	connection.disconnect();
		    	    }
		    	    
		    	    return null;
		    	}
		};
		this.transport.debug = false; // true;
		//-----------------------------------------------------------
		this.getForumList = new SimpleWebMethodNS<ForumEntity>("GetForumList") {
			private Runnable buildForum;
			private Runnable buildForumGroup;
			private EntityReceiver<? super ForumEntity> recv;
			@Override
			public void call(final EntityReceiver<? super ForumEntity> recv) {
				this.recv = recv;
				this.buildForum = new Runnable() {
					@Override
					public void run() {
						mode = 0;
						final Forum item = forumBuilder.build();
						setNotContinueFlag(!recv.consume(item));
					}
				};
				this.buildForumGroup = new Runnable() {
					@Override
					public void run() {
						mode = 0;
						final ForumGroup item = forumGroupBuilder.build();
						setNotContinueFlag(!recv.consume(item));
					}
				};
				while (true) {
					final SoapObject forumRequest = new SoapObject("", "forumRequest");
					putCredentials(forumRequest);
					if (!call(forumRequest, RowVersion.Forums))
						break;
				}
			}
			
			final ForumBuilder forumBuilder = ForumBuilder.create();
			final ForumGroupBuilder forumGroupBuilder = ForumGroupBuilder.create();
			String currentElement;
			int mode;
			
			@Override
			protected void startElement(final String name, final Attributes attributes, final Deque<Runnable> stackFrames) {
				this.currentElement = name;
				if ("JanusForumInfo".equals(currentElement)) {
					mode = 1;
					stackFrames.push(buildForum);
				}
				else if ("JanusForumGroupInfo".equals(currentElement)) {
					mode = 2;
					stackFrames.push(buildForumGroup);
				}
			}
			@Override
			protected void characters(final String characters) {
				switch (mode) {
				default: return;
				case 1:
					if ("forumId".equals(currentElement))
						forumBuilder.id(Converters.asLong(characters));
					else if ("forumGroupId".equals(currentElement))
						forumBuilder.groupId(Converters.asLong(characters));
					else if ("shortForumName".equals(currentElement))
						forumBuilder.shortName(characters);
					else if ("forumName".equals(currentElement))
						forumBuilder.fullName(characters);
					break;
				case 2:
					if ("forumGroupId".equals(currentElement))
						forumGroupBuilder.id(Converters.asLong(characters));
					else if ("forumGroupName".equals(currentElement))
						forumGroupBuilder.name(characters);
					break;
				}
				currentElement = null;
			}

			@Override
			protected void flush() {
				recv.flush();
			}

			@Override
			protected void resetModel() {
				currentElement = null;
				mode = 0;
			}
		};
		//-----------------------------------------------------------
		this.getNewData = new MessageNSMethod("GetNewData") {
			@Override
			public void call(final EntityReceiver<? super MessageEntity> recv) {
				this.recv = recv;
				
				final Set<Long> lastRowVersionSkip = new HashSet<Long>();
				while (true) {
					final ForumRowVersion messageRowIds = messageRowVersion.getTopForumRowVersion(lastRowVersionSkip);
					if (messageRowIds == null) {
						// Stop if no forums selected
						break;
					}
					final ForumRowVersion ratingRowIds = messageRowVersion.getConnectedObject(messageRowIds, RowVersion.Rating);
					final ForumRowVersion moderateRowIds = messageRowVersion.getConnectedObject(messageRowIds, RowVersion.Moderate);
					
					responseNames.put(messageRowIds.getResponseName(), messageRowIds);
					responseNames.put(ratingRowIds.getResponseName(), ratingRowIds);
					responseNames.put(moderateRowIds.getResponseName(), moderateRowIds);
					
					final SoapObject changeRequest = new SoapObject("", "changeRequest");
					putCredentials(changeRequest);
	
					final SoapObject subscribedForums = new SoapObject("", "subscribedForums");
					final boolean isFirstRequest = messageRowIds.getRaw() == 0 && !forceFullHistory;
					changeRequest.addSoapObject(subscribedForums);
					for (final long forumId : messageRowIds.getForumIds()) {
						final SoapObject RequestForumInfo = new SoapObject("", "RequestForumInfo");
						RequestForumInfo.addProperty("forumId", forumId);
						RequestForumInfo.addProperty("isFirstRequest", isFirstRequest);
						subscribedForums.addSoapObject(RequestForumInfo);
					}
					
//					final SoapObject breakMsgIds = new SoapObject("", "breakMsgIds");
//					changeRequest.addSoapObject(breakMsgIds);
//					if (breakMsgIds_ != null) {
//						for (final long breakMsgId : breakMsgIds_) {
//							Log.d(TAG, "breakMsgId: " + breakMsgId);
//							breakMsgIds.addProperty("int", breakMsgId);
//						}
//						breakMsgIds_ = null;
//					}
					
					changeRequest.addProperty("maxOutput", 0);
					if (!call(changeRequest, ratingRowIds, messageRowIds, moderateRowIds)) {
						lastRowVersionSkip.add(messageRowIds.getRaw());
						if (!messageRowIds.incomplete())
							break;
					}
				};
			}
		};
		//-----------------------------------------------------------
		this.getBrokenTopics = new MessageNSMethod("GetTopicByMessage") {
			@Override
			public void call(final EntityReceiver<? super MessageEntity> recv) {
				this.recv = recv;
				
				while (true) {
					final Iterable<Long> breakTopicIds_ = messageRowVersion.getBreakTopicIds();
					if (breakTopicIds_ == null) {
						// Stop if no broken forum ids found
						break;
					}
					
					final SoapObject topicRequest = new SoapObject("", "topicRequest");
					putCredentials(topicRequest);
	
					final SoapObject messageIds = new SoapObject("", "messageIds");
					topicRequest.addSoapObject(messageIds);
					for (final long breakTopicId : breakTopicIds_) {
						Log.d(TAG, "breakTopicId: " + breakTopicId);
						messageIds.addProperty("int", breakTopicId);
					}
					
					if (!call(topicRequest)) {
						break;
					}
				};
			}
		};
		//-----------------------------------------------------------
		this.postChange = new SimpleWebMethodNS<Entity>("PostChange") {
			@Override
			protected void characters(String characters) {
			}
			@Override
			protected void startElement(String name, Attributes attributes,
					Deque<Runnable> stackFrames) {
			}
			@Override
			protected void flush() {
			}
			@Override
			public void call(EntityReceiver<? super Entity> recv) {
				final Iterable<ComposedMessage> writtenMsgs = composedMessages.getAll();
				if (!writtenMsgs.iterator().hasNext())
					return;
				
				final SoapObject postRequest = new SoapObject("", "postRequest");
				putCredentials(postRequest);
				
				final SoapObject writtenMessages = new SoapObject("", "writedMessages");
				postRequest.addSoapObject(writtenMessages);
				
				for (final ComposedMessage msg : writtenMsgs) {
					final SoapObject postMessageInfo = new SoapObject("", "PostMessageInfo");
					writtenMessages.addSoapObject(postMessageInfo);

					final long localMessageId = msg.getId();
					postMessageInfo.addProperty("localMessageId", localMessageId);
					postMessageInfo.addProperty("parentId", msg.getParentId() != null ? msg.getParentId() : 0);
					postMessageInfo.addProperty("forumId", msg.getForumId());
					postMessageInfo.addProperty("subject", msg.getSubj());
					postMessageInfo.addProperty("message", msg.getBody());
				}
				
				call(postRequest);
			}
		};
		//-----------------------------------------------------------
		this.postChangeCommit = new SimpleWebMethodNS<Entity>("PostChangeCommit") {
			String currentElement;
			int mode;
			final List<Long> sentIds = new ArrayList<Long>();
			String exceptionString;
			String exceptionInfo;
			long localMsgId;
			
			@Override
			protected void characters(String characters) {
				Log.d(TAG, characters);
				if (mode == 2) {
					final long id = Long.parseLong(characters);
					sentIds.add(id);
				} else if (mode == 3) {
					if ("exception".equals(currentElement)) {
						exceptionString = characters;
					} else if ("localMessageId".equals(currentElement)) {
						localMsgId = Long.parseLong(characters);
					} else if ("info".equals(currentElement)) {
						exceptionInfo = characters;
					}
				}
			}
			@Override
			protected void startElement(String name, Attributes attributes,
					Deque<Runnable> stackFrames) {
				Log.d(TAG, name);
				this.currentElement = name;
				if (mode == 0 && "commitedIds".equals(currentElement)) {
					mode = 1;
					stackFrames.push(cleanCommitted);
				} else if (mode == 0 && "PostExceptionInfo".equals(currentElement)) { 
					mode = 3;
					stackFrames.push(notifyError);
				} else if (mode == 1 && "int".equals(currentElement)) {
					mode = 2;
				}
			}
			final Runnable cleanCommitted = new Runnable() {
				@Override
				public void run() {
					composedMessages.deleteByIds(sentIds.toArray(new Long[sentIds.size()]));
					sentIds.clear();
					mode = 0;
				}};
				final Runnable notifyError = new Runnable() {
				@Override
				public void run() {
					composedMessages.deleteByIds(localMsgId);
					Log.e(TAG, Converters.nonNullStr(exceptionString) + " : " + Converters.nonNullStr(exceptionInfo));
					exceptionString = null;
					exceptionInfo = null;
					localMsgId = 0;
					mode = 0;
				}};
			
				
			@Override
			protected void flush() {
				composedMessages.deleteByIds(sentIds.toArray(new Long[sentIds.size()]));
				sentIds.clear();
			}
			@Override
			public void call(EntityReceiver<? super Entity> recv) {
				call((SoapObject)null);
			}
		};

		//-----------------------------------------------------------
		this.getNewUsers = new SimpleWebMethodNS<UserEntity>("GetNewUsers") {
			Runnable buildUser;
			EntityReceiver<? super UserEntity> recv;
			@Override
			public void call(final EntityReceiver<? super UserEntity> recv) {
				this.recv = recv;
				this.buildUser = new Runnable() {
					@Override
					public void run() {
						mode = 0;
						final User item = userBuilder.build();
						setNotContinueFlag(!recv.consume(item));
					}
				};
				while (true) {
					final SoapObject userRequest = new SoapObject("", "userRequest");
					putCredentials(userRequest);
					userRequest.addProperty("maxOutput", 0);
					if (!call(userRequest, RowVersion.Users))
						break;
				}
			}

			final UserBuilder userBuilder = UserBuilder.create();
			String currentElement;
			int mode;
			
			@Override
			protected void characters(final String characters) {
				if (mode == 1) {
					if ("userId".equals(currentElement))
						userBuilder.id(Converters.asLong(characters));
					else if ("userName".equals(currentElement))
						userBuilder.name(characters);
					else if ("userNick".equals(currentElement))
						userBuilder.nick(characters);
					else if ("realName".equals(currentElement))
						userBuilder.realName(characters);
					else if ("publicEmail".equals(currentElement))
						userBuilder.email(characters);
					else if ("homePage".equals(currentElement))
						userBuilder.www(characters);
					else if ("specialization".equals(currentElement))
						userBuilder.specialization(characters);
					else if ("whereFrom".equals(currentElement))
						userBuilder.whereFrom(characters);
					else if ("origin".equals(currentElement))
						userBuilder.origin(characters);
					else if ("userClass".equals(currentElement))
						userBuilder.role(Converters.asLong(characters));
				}
				currentElement = null;
			}
			@Override
			protected void startElement(final String name, final Attributes attributes, final Deque<Runnable> stackFrames) {
				this.currentElement = name;
				if ("JanusUserInfo".equals(currentElement)) {
					mode = 1;
					stackFrames.push(buildUser);
				}
			}
			@Override
			protected void resetModel() {
				mode = 0;
				currentElement = null;
			}
			@Override
			protected void flush() {
				recv.flush();
			}
		};
		//-----------------------------------------------------------
		//-----------------------------------------------------------
		//-----------------------------------------------------------
		//-----------------------------------------------------------
		//-----------------------------------------------------------
	}
	
	private abstract class MessageNSMethod extends SimpleWebMethodNS<MessageEntity> {
		protected EntityReceiver<? super MessageEntity> recv;
		private final Runnable buildMessage = new Runnable() {
			@Override
			public void run() {
				mode = 0;
				final Message item = builder.build();
//				debugOut.println(item);
//				debugOut.flush();
				setNotContinueFlag(!recv.consume(item));
			}};


		protected MessageNSMethod(String methodName) {
			super(methodName);
		}
		final MessageBuilder builder = MessageBuilder.create();
		String currentElement;
		int mode;
		
		@Override
		protected void characters(final String characters) {
			if (mode == 1) {
				if ("messageId".equals(currentElement))
					builder.id(Converters.asLong(characters));
				else if ("topicId".equals(currentElement))
					builder.topicId(Converters.asLong(characters));
				else if ("parentId".equals(currentElement))
					builder.parentId(Converters.asLong(characters));
				else if ("userId".equals(currentElement))
					builder.userId(Converters.asLong(characters));
				else if ("forumId".equals(currentElement))
					builder.forumId(Converters.asLong(characters));
				else if ("subject".equals(currentElement))
					builder.subject(characters);
				else if ("message".equals(currentElement))
					builder.body(characters);
				else if ("articleId".equals(currentElement))
					builder.articleId(Converters.asLong(characters));
				else if ("messageDate".equals(currentElement))
					builder.messageDate(Converters.asDate(characters));
				else if ("lastModerated".equals(currentElement))
					builder.lastModerated(Converters.asDate(characters));
				else if ("userNick".equals(currentElement))
					builder.userName(characters);
			}
			currentElement = null;
		}

		@Override
		protected void startElement(final String name, final Attributes attributes, final Deque<Runnable> stackFrames) {
			currentElement = name;
			if ("JanusMessageInfo".equals(currentElement)) {
				mode = 1;
				stackFrames.push(buildMessage);
			}
		}
		@Override
		protected void resetModel() {
			mode = 0;
			currentElement = null;
		}
		@Override
		protected void flush() {
			recv.flush();
		}
		
	};
	
	
	
	protected void readResponse(final String soapAction, final InputStream is) throws SAXException, IOException {
		final ContentHandler contentHandler = contentHandlers.get(soapAction);
		final XMLReader reader = XMLReaderFactory.createXMLReader();

		reader.setContentHandler(contentHandler);
		if (contentHandler instanceof ErrorHandler) {
			reader.setErrorHandler((ErrorHandler) contentHandler);
		}
		reader.parse(new InputSource(is));
		//reader.parse(new InputSource(cacheToTemporary(is)));
	}

//	private static final String tempFileName = DataModel.getStorageDirectory() + File.separator + "rsdn.home.log";
//	
//	protected InputStream cacheToTemporary(final InputStream is) throws IOException {
//		final File f = new File(tempFileName);
//		if (f.exists())
//			f.delete();
//		final FileOutputStream bos = new FileOutputStream(f, true);
//		//f.deleteOnExit();
//		int bitesRead = 0;
//        try {
//			final byte[] buf = new byte[1024 * 1024];
//	        
//	        while (true) {
//	            int rd = is.read(buf, 0, buf.length - 1);
//	            if (rd == -1)
//	                break;
//	            buf[rd] = 0;
//	            bos.write(buf, 0, rd);
//	            //final String string = new String(buf);
//				//System.err.print(string);
//	            bitesRead += rd;
//	        }
//	        
//	        bos.flush();
//        } finally {
//	        System.err.flush();
//        	System.err.println();
//        	System.err.println();
//        	System.err.println(String.format("Bites read: %d", bitesRead));
//        	bos.close();
//        	is.close();
//        }
//        return new FileInputStream(f);
//	}

	public void setCredentials(final Credentials credentials) {
		this.credentials = credentials;
	}
	
	private void putCredentials(final SoapObject forumRequest) {
		forumRequest.addProperty("userName", credentials.getUserName());
		forumRequest.addProperty("password", credentials.getPassword());
	}

	private abstract class SimpleWebMethodNS<E extends Entity> extends SimpleWebMethod<E> {
		
		protected SimpleWebMethodNS(final String methodName) {
			super(RsdnWebService.SOAP_ACTION_NS, methodName);
			RsdnWebService.this.contentHandlers.put(methodName, contentHandler);
		}
		
		@Override
		protected Transport getTransport() {
			return transport;
		}
		
		protected final Map<String, RowVersionProvider> responseNames = new HashMap<String, RowVersionProvider>(RowVersion.responseNames);
		
		private final DefaultHandler contentHandler = new DefaultHandler() {
			final LinkedList<Runnable> stackFrames = new LinkedList<Runnable>();
			RowVersionProvider ver;
			boolean inActionNS;
			
			@Override
			public void startDocument() throws org.xml.sax.SAXException {
				receivedRowVersion.clear();
				stackFrames.clear();
				ver = null;
				inActionNS = false;
				SimpleWebMethodNS.this.resetModel();
			};
			@Override
			public void endDocument() throws SAXException {
				SimpleWebMethodNS.this.flush();
			}
			@Override
			public void startElement(final String uri, final String localName, final String qName,
					final org.xml.sax.Attributes attributes) throws SAXException { 
				stackFrames.push(null);
				if (!inActionNS) {
					if (SOAP_ACTION_NS.equals(uri))
						inActionNS = true;
					else
						return;
				}
				final RowVersionProvider ver = responseNames.get(localName);
				if (ver != null) {
					this.ver = ver;
				} else {
					SimpleWebMethodNS.this.startElement(localName, attributes, stackFrames);
				}
			};
			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				if (!inActionNS) return;
				final String characters = String.valueOf(ch, start, length);
				if (this.ver != null) {
					receivedRowVersion.put(this.ver, characters);
					this.ver = null;
				} else {
					SimpleWebMethodNS.this.characters(characters);
				}
			}
			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				while (!stackFrames.isEmpty()) {
					final Runnable frame = stackFrames.pop();
					if (frame == null)
						break;
					frame.run();
				}
			}
		};
		
		private final Map<RowVersionProvider, String> receivedRowVersion = new HashMap<RowVersionProvider, String>();
		
		@Override
		protected Map<RowVersionProvider, String> getReceivedRowVersion() {
			return Collections.unmodifiableMap(receivedRowVersion);
		}

		protected abstract void characters(final String characters) ;

		protected abstract void startElement(String name, Attributes attributes, Deque<Runnable> stackFrames);

		protected abstract void flush();

		protected void resetModel() {}
	}
	
	
}
