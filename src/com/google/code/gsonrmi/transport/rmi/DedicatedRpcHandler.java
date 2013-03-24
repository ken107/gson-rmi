package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;

public class DedicatedRpcHandler extends Thread implements RpcHandler {
	
	private final RpcHandler handler;
	private final Transport transport;
	private final BlockingQueue<Object[]> mq;
	
	public DedicatedRpcHandler(RpcHandler handler, Transport transport) {
		this.handler = handler;
		this.transport = transport;
		mq = new LinkedBlockingQueue<Object[]>();
	}

	@Override
	public RpcResponse handle(RpcRequest request, URI dest, Route src) {
		mq.add(new Object[] {request, dest, src});
		return null;
	}

	@Override
	public void handle(RpcResponse response, Call callback) {
		mq.add(new Object[] {response, callback});
	}
	
	@Override
	public void shutdown() {
		interrupt();
		handler.shutdown();
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				Object[] m = mq.take();
				if (m[0] instanceof RpcRequest) {
					RpcResponse response = handler.handle((RpcRequest) m[0], (URI) m[1], (Route) m[2]);
					if (response != null) transport.send(new Message(null, Arrays.asList((Route) m[2]), response));
				}
				else if (m[0] instanceof RpcResponse) handler.handle((RpcResponse) m[0], (Call) m[1]);
			}
		}
		catch (InterruptedException e) {
		}
	}

	@Override
	public void periodicCleanup() {
		handler.periodicCleanup();
	}
}
