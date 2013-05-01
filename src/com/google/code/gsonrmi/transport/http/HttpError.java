package com.google.code.gsonrmi.transport.http;

import com.google.code.gsonrmi.RpcError;

public interface HttpError {

	public static final RpcError HTTP_REQUEST_FAILED = new RpcError(-32020, "HTTP request failed");
	public static final RpcError IO_EXCEPTION = new RpcError(-32021, "HTTP I/O Exception");
}
