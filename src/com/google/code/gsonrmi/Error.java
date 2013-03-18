package com.google.code.gsonrmi;

public class Error {

	public final int code;
	public final String message;
	public final Parameter data;
	
	public Error(int code, String message) {
		this.code = code;
		this.message = message;
		this.data = null;
	}
	
	public Error(int code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = new Parameter(data);
	}
}
