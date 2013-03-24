package com.google.code.gsonrmi;

public class RpcError {
	
	public static final RpcError PARSER_ERROR = new RpcError(-32700, "Parser error");
	public static final RpcError INVALID_REQUEST = new RpcError(-32600, "Invalid request");
	public static final RpcError METHOD_NOT_FOUND = new RpcError(-32601, "Method not found");
	public static final RpcError INVALID_PARAMS = new RpcError(-32602, "Invalid params");
	public static final RpcError INTERNAL_ERROR = new RpcError(-32603, "Internal error");
	public static final RpcError INVOCATION_EXCEPTION = new RpcError(-32000, "Invocation exception");
	public static final RpcError PARAM_VALIDATION_FAILED = new RpcError(-32001, "Parameter validation failed");

	public final int code;
	public final String message;
	public final Parameter data;
	
	public RpcError(int code, String message) {
		this(code, message, null);
	}
	
	public RpcError(int code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = data != null ? new Parameter(data) : null;
	}
	
	public RpcError(RpcError error, Object data) {
		this(error.code, error.message, data);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof RpcError && ((RpcError) o).code == code;
	}
	
	@Override
	public int hashCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return code + " " + message;
	}
}
