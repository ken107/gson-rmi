package com.google.code.gsonrmi.transport.rmi;

import com.google.code.gsonrmi.RpcError;

public interface RmiError {

	public static final RpcError TARGET_NOT_FOUND = new RpcError(-32010, "Target not found");
	public static final RpcError UNREACHABLE = new RpcError(-32011, "Unreachable");
}
