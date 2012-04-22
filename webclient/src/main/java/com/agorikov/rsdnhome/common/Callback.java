package com.agorikov.rsdnhome.common;

public interface Callback<P, R> {
	R call(P param);
}
