package com.agorikov.rsdnhome.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public abstract class LazyCollection<E> implements Iterable<E>, Iterator<E> {

	protected final ResultSet resultSet;
	private boolean hasNext_;
	
	public LazyCollection(final ResultSet resultSet) {
		this.resultSet = resultSet;
		doNext();
	}
	
	@Override
	public final boolean hasNext() {
		return hasNext_;
	}

	protected final void doNext() {
		try {
			hasNext_ = resultSet.next();
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public final Iterator<E> iterator() {
		return this;
	}
	
	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}
}
