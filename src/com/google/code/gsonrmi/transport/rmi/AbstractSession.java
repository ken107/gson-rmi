package com.google.code.gsonrmi.transport.rmi;

public class AbstractSession {

	String id;
	public long lastAccessed;
	public int expireSec;
	public boolean invalid;
	
	public boolean isInvalid() {
		return invalid || expireSec > 0 && System.currentTimeMillis()-lastAccessed > expireSec*1000;
	}
	
	protected void onRemove() {
	}
}
