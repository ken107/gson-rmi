package com.google.code.gsonrmi;

public class RpcError {

	public final int code;
	public final String message;
	public final Parameter data;
	
	public RpcError(int code, String message) {
		this.code = code;
		this.message = message;
		this.data = null;
	}
	
	public RpcError(int code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = new Parameter(data);
	}
}
