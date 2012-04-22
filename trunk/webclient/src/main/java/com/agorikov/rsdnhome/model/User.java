package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public final class User implements UserEntity {

	final long id;
	final String name;
	final String realName;
	final String email;
	final String www;
	final String specialization;
	final String whereFrom;
	final String origin;
	final Long role;
	
	private User(long id, String name, String realName, String email, String www, String specialization, String where_from, String origin, Long role) {
		this.id = id;
		this.name = name;
		this.realName = realName;
		this.email = email;
		this.www = www;
		this.specialization = specialization;
		this.whereFrom = where_from;
		this.origin = origin;
		this.role = role;
	}
	
	public long getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getRealName() {
		return realName;
	}
	public String getEmail() {
		return email;
	}
	public String getWww() {
		return www;
	}
	public String getSpecialization() {
		return specialization;
	}
	public String getWhereFrom() {
		return whereFrom;
	}
	public String getOrigin() {
		return origin;
	}
	public Long getRole() {
		return role;
	}
	
	@Override
	public String toString() {
		return String.format("User %s: %s, real name: \"%s\", role: %s, www: %s, email: %s; from %s", 
				getId(), getName(), getRealName(), getRole(), getWww(), getEmail(), getWhereFrom());
	}
	
	public static abstract class UserBuilder implements Builder<User> {

		public static UserBuilder create() {
			return new UserBuilder() {};
		}
		
		private Long id;
		private String name;
		private String nick;
		private String realName;
		private String email;
		private String www;
		private String specialization;
		private String whereFrom;
		private String origin;
		private Long role;
		
		public UserBuilder id(long id) {
			this.id = id;
			return this;
		}
		public UserBuilder name(String name) {
			this.name = name;
			return this;
		}
		public UserBuilder nick(String nick) {
			this.nick = nick;
			return this;
		}
		public UserBuilder realName(String realName) {
			this.realName = realName;
			return this;
		}
		public UserBuilder email(String email) {
			this.email = email;
			return this;
		}
		public UserBuilder www(String www) {
			this.www = www;
			return this;
		}
		public UserBuilder specialization(String specialization) {
			this.specialization = specialization;
			return this;
		}
		public UserBuilder whereFrom(String whereFrom) {
			this.whereFrom = whereFrom;
			return this;
		}
		public UserBuilder origin(String origin) {
			this.origin = origin;
			return this;
		}
		public UserBuilder role(Long role) {
			this.role = role;
			return this;
		}
		
		void reset() {
			this.id = null;
			this.name = null;
			this.nick = null;
			this.realName = null;
			this.email = null;
			this.www = null;
			this.specialization = null;
			this.whereFrom = null;
			this.origin = null;
			this.role = null;
		}
		
		@Override
		public User build() {
			try {
				return new User(id, (nick != null && nick.length() != 0) ? nick : name, realName, email, www, specialization, whereFrom, origin, role);
			} finally {
				reset();
			}
		}
		
		
	}
	
	
	
	
	
	
	
	
}
