package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;

import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Route;

public interface RpcHandler {

	RpcResponse handle(RpcRequest request, URI dest, Route src);
	void handle(RpcResponse response, Call callback);
	void shutdown();
}
