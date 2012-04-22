package com.agorikov.rsdnhome.model;

public interface RowVersionProvider {
	String get();
	void put(String base64);
	String getRequestName();
	String getResponseName();

}
