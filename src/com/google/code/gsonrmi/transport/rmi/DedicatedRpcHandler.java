package com.google.code.gsonrmi.transport.rmi;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;

public class DedicatedRpcHandler implements RpcHandler {
	
	private final RpcHandler handler;
	private final Transport transport;
	private final Executor executor;
	
	public DedicatedRpcHandler(RpcHandler handler, Transport transport) {
		this(handler, transport, Executors.newSingleThreadExecutor());
	}
	
	public DedicatedRpcHandler(RpcHandler handler, Transport transport, Executor executor) {
		this.handler = handler;
		this.transport = transport;
		this.executor = executor;
	}
	
	public void start() {
	}

	@Override
	public RpcResponse handle(RpcRequest request, Route dest, Route src) {
		executor.execute(new RequestHandler(request, dest, src));
		return null;
	}

	@Override
	public void handle(RpcResponse response, Route dest, List<Route> srcs, Callback callback) {
		executor.execute(new ResponseHandler(response, dest, srcs, callback));
	}
	
	@Override
	public void shutdown() {
		executor.execute(new ShutdownHandler());
	}

	@Override
	public void periodicCleanup() {
		executor.execute(new CleanupHandler());
	}
	
	protected class RequestHandler implements Runnable {
		private final RpcRequest request;
		private final Route dest;
		private final Route src;
		
		public RequestHandler(RpcRequest request, Route dest, Route src) {
			this.request = request;
			this.dest = dest;
			this.src = src;
		}
		
		@Override
		public void run() {
			RpcResponse response = handler.handle(request, dest, src);
			if (response != null) {
				if (response.id != null) transport.send(new Message(dest, Arrays.asList(src), response));
				else {
					if (response.error != null) {
						System.err.println("Notification failed:  " + dest.hops[0] + " method " + request.method + ", " + response.error);
						if (response.error.equals(RpcError.INVOCATION_EXCEPTION)) response.error.data.getValue(Exception.class, null).printStackTrace();
					}
				}
			}
		}
	}
	
	protected class ResponseHandler implements Runnable {
		private final RpcResponse response;
		private final Route dest;
		private final List<Route> srcs;
		private final Callback callback;
		
		public ResponseHandler(RpcResponse response, Route dest, List<Route> srcs, Callback callback) {
			this.response = response;
			this.dest = dest;
			this.srcs = srcs;
			this.callback = callback;
		}
		
		@Override
		public void run() {
			handler.handle(response, dest, srcs, callback);
		}
	}
	
	protected class ShutdownHandler implements Runnable {
		@Override
		public void run() {
			handler.shutdown();
		}
	}
	
	protected class CleanupHandler implements Runnable {
		@Override
		public void run() {
			handler.periodicCleanup();
		}
	}
}
