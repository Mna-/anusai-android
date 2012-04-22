package com.agorikov.rsdnhome.model;

import com.agorikov.rsdnhome.common.Builder;

public final class ForumGroup implements ForumEntity {

	private final long id;
	private final String name;
	
	private ForumGroup(final long id, final String name) {
		this.id = id;
		this.name = name;
	}
	
	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return String.format("{%s : %s}", getId(), getName());
	}
	
	public static abstract class ForumGroupBuilder implements Builder<ForumGroup> {
		
		private Long id;
		private String name;
		
		public final ForumGroupBuilder id(final long id) {
			this.id = id;
			return this;
		}
		public final ForumGroupBuilder name(final String name) {
			this.name = name;
			return this;
		}
		
		void reset() {
			this.id = null;
			this.name = null;
		}
		
		public static ForumGroupBuilder create() {
			return new ForumGroupBuilder() {};
		}
		
		@Override
		public final ForumGroup build() {
			try {
				return new ForumGroup(id, name);
			} finally {
				reset();
			}
		}
	}
	
	
	
}
