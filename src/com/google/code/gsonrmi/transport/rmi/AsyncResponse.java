package com.google.code.gsonrmi.transport.rmi;

import java.util.Arrays;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;

public class AsyncResponse {
	
	protected RpcRequest request;
	protected Route dest;
	protected Route src;
	protected RpcResponse response;
	protected Transport transport;

	protected void setRequest(RpcRequest request, Route dest, Route src) {
		this.request = request;
		this.dest = dest;
		this.src = src;
		trySend();
	}
	
	public void send(Object result, Transport transport) {
		response = new RpcResponse();
		response.result = result != null ? new Parameter(result) : null;
		this.transport = transport;
		trySend();
	}
	
	public void sendException(Exception exception, Transport transport) {
		response = new RpcResponse();
		response.error = new RpcError(RpcError.INVOCATION_EXCEPTION, exception);
		this.transport = transport;
		trySend();
	}
	
	protected void trySend() {
		if (request != null && response != null) {
			response.id = request.id;
			if (response.id != null) transport.send(new Message(dest, Arrays.asList(src), response));
			else {
				if (response.error != null) {
					System.err.println("Notification failed:  " + dest.hops[0] + " method " + request.method);
					response.error.data.getValue(Exception.class, null).printStackTrace();
				}
			}
			request = null;
			response = null;
		}
	}
}
