package com.agorikov.rsdnhome.webclient.model;

import com.agorikov.rsdnhome.model.Entity;

public abstract class SimpleWebMethod<E extends Entity> extends WebMethod {
	protected SimpleWebMethod(String SOAP_ACTION_NS, String methodName) {
		super(SOAP_ACTION_NS, methodName);
	}

	public interface EntityReceiver<E extends Entity> {
		boolean consume(E e);
		void flush();
	}
	
	public abstract void call(EntityReceiver<? super E> recv);
	
}
