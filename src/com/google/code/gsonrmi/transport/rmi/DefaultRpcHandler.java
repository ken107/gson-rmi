package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.util.Arrays;

import com.google.code.gsonrmi.Invoker;
import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Route;

public class DefaultRpcHandler implements RpcHandler {
	
	private final Object target;
	private final Invoker invoker;
	
	public DefaultRpcHandler(Object target, Invoker invoker) {
		this.target = target;
		this.invoker = invoker;
	}

	@Override
	public RpcResponse handle(RpcRequest request, URI dest, Route src) {
		return invoker.doInvoke(request, target, null);
	}

	@Override
	public void handle(RpcResponse response, Call callback) {
		RpcRequest request = new RpcRequest();
		request.method = callback.method;
		request.params = Arrays.copyOf(callback.params, callback.params.length+2);
		request.params[request.params.length-2] = response.result;
		request.params[request.params.length-1] = new Parameter(response.error);
		
		RpcResponse r = invoker.doInvoke(request, target, null);
		if (r.error != null) {
			System.err.println("Invoke response failed: " + r.error);
			if (r.error.code == -32000) r.error.data.getValue(Exception.class, null).printStackTrace();
		}
	}
	
	@Override
	public void shutdown() {
	}

}
