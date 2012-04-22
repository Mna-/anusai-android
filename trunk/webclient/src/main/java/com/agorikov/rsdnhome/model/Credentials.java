package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public final class Credentials {

	private final String userName;
	private final String password;
	
	private Credentials(final String userName, String password) {
		this.userName = userName;
		this.password = password;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public String getPassword() {
		return password;
	}
	
	
	public static abstract class CredentialsBuilder implements Builder<Credentials> {
		
		private String userName;
		private String password;
		
		public final CredentialsBuilder userName(final String userName) {
			this.userName = userName;
			return this;
		}
		
		public final CredentialsBuilder password(final String password) {
			this.password = password;
			return this;
		}
		
		public static CredentialsBuilder create() {
			return new CredentialsBuilder() {
			};
		}
		
		void reset() {
			this.userName = null;
			this.password = null;
		}
		
		@Override
		public Credentials build() {
			return new Credentials(userName, password);
		}
		
	}
	
}
