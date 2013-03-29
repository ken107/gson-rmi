package com.google.code.gsonrmi.transport.rmi;

import com.google.code.gsonrmi.RpcError;

public class RmiError {

	public static final RpcError TARGET_NOT_FOUND = new RpcError(-32010, "Target not found");
	public static final RpcError UNREACHABLE = new RpcError(-32011, "Unreachable");
	public static final RpcError RESPONSE_TIMEOUT = new RpcError(-32012, "Response timeout");
}
