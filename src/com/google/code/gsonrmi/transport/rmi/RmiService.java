package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Proxy;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.Transport.DeliveryFailure;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public class RmiService extends Thread {

	public static final String SCHEME = "rmi";
	
	private final URI addr;
	private final BlockingQueue<Message> mq;
	private final Transport t;
	private final Gson gson;
	private final Map<String, RpcHandler> handlers;
	private final Map<Integer, Call> pendingCalls;
	private int idGen;
	
	public RmiService(Transport transport, Gson deserializer) throws URISyntaxException {
		this(transport, deserializer, new Options());
	}
	
	public RmiService(Transport transport, Gson deserializer, Options options) throws URISyntaxException {
		addr = new URI(SCHEME, "service", null);
		mq = new LinkedBlockingQueue<Message>();
		t = transport;
		t.register(SCHEME, mq);
		t.sendEvery(new Message(null, Arrays.asList(new Route(addr)), new PeriodicCleanup()), options.cleanupInterval, options.cleanupInterval);
		gson = deserializer;
		handlers = new HashMap<String, RpcHandler>();
		handlers.put("service", new DefaultRpcHandler(this, gson));
		pendingCalls = new HashMap<Integer, Call>();
	}
	
	@RMI
	public URI register(String id, Object target) throws URISyntaxException {
		if (target instanceof RpcHandler) handlers.put(id, (RpcHandler) target);
		else handlers.put(id, new DefaultRpcHandler(target, gson));
		return new URI(SCHEME, id, null);
	}
	
	private void process(Message m) {
		if (m.contentOfType(Call.class)) handle(m.getContentAs(Call.class, gson));
		else if (m.contentOfType(RpcRequest.class)) handle(m.getContentAs(RpcRequest.class, gson), m.dests, m.src);
		else if (m.contentOfType(RpcResponse.class)) handle(m.getContentAs(RpcResponse.class, gson), m.dests.get(0), Arrays.asList(m.src));
		else if (m.contentOfType(DeliveryFailure.class)) handle(m.getContentAs(DeliveryFailure.class, gson), m.dests.get(0), m.src);
		else if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else if (m.contentOfType(PeriodicCleanup.class)) handle(m.getContentAs(PeriodicCleanup.class, gson));
		else if (m.contentOfType(Proxy.CheckConnection.class));
		else if (m.contentOfType(Proxy.OnConnectionClosed.class));
		else System.err.println("Unhandled message type: " + m.contentType);
	}
	
	private void handle(Call m) {
		m.timeSent = System.currentTimeMillis();
		if ("_onConnectionClosed".equals(m.method)) {
			if (m.params.length != 0) System.err.println("WARN: _onConnectionClosed accepts no params");
			if (m.callback != null) {
				Proxy.OnConnectionClosed request = new Proxy.OnConnectionClosed();
				Parameter[] data = Arrays.copyOf(m.callback.params, m.callback.params.length+1);
				data[data.length-1] = new Parameter(m.callback.method);
				request.data = new Parameter(data);
				t.send(new Message(m.callback.target, m.targets, request));
			}
			else System.err.println("_onConnectionClosed requires a callback");
		}
		else {
			Integer id = null;
			if (m.callback != null) {
				id = ++idGen;
				pendingCalls.put(id, m);
			}
			if ("_checkConnection".equals(m.method)) {
				if (m.callback != null) {
					Proxy.CheckConnection request = new Proxy.CheckConnection();
					request.data = new Parameter(id);
					t.send(new Message(m.callback.target, m.targets, request));
				}
				else System.err.println("_checkConnection requires a callback");
			}
			else {
				RpcRequest request = new RpcRequest();
				request.method = m.method;
				request.params = m.params;
				request.id = id != null ? new Parameter(id) : null;
				t.send(new Message(m.callback != null ? m.callback.target : new Route(addr), m.targets, request));
			}
		}
	}
	
	private void handle(RpcRequest request, List<Route> dests, Route src) {
		for (Route dest : dests) {
		RpcResponse response;
		URI targetUri = dest.hops.getFirst();
		RpcHandler handler = handlers.get(targetUri.getSchemeSpecificPart());
		if (handler != null) response = handler.handle(request, dest, src);
		else {
			response = new RpcResponse();
			response.id = request.id;
			response.error = new RpcError(RmiError.TARGET_NOT_FOUND, targetUri);
		}
		if (response != null) {
			if (response.id != null) t.send(new Message(dest, Arrays.asList(src), response));
			else {
				if (response.error != null) {
					System.err.println("Notification failed:  " + targetUri + " method " + request.method + ", " + response.error);
					if (response.error.equals(RpcError.INVOCATION_EXCEPTION)) response.error.data.getValue(Exception.class, gson).printStackTrace();
				}
			}
		}
		}
	}
	
	private void handle(RpcResponse response, Route dest, List<Route> srcs) {
		Integer responseId = response.id.getValue(Integer.class, gson);
		Call pendingCall = pendingCalls.get(responseId);
		if (pendingCall != null) invokeCallback(pendingCall.callback, response, dest, srcs);
		else System.err.println("No pending request with id " + responseId);
	}
	
	private void handle(DeliveryFailure m, Route dest, Route src) {
		if (m.message.contentOfType(RpcRequest.class)) {
			RpcRequest request = m.message.getContentAs(RpcRequest.class, gson);
			RpcResponse response = new RpcResponse();
			response.id = request.id;
			response.error = RmiError.UNREACHABLE;
			prependToEach(m.message.dests, src);
			handle(response, dest, m.message.dests);
		}
		else if (m.message.contentOfType(RpcResponse.class)) {
			RpcResponse response = m.message.getContentAs(RpcResponse.class, gson);
			Integer responseId = response.id.getValue(Integer.class, gson);
			System.err.println("Delivery failed for response with id " + responseId);
		}
		else if (m.message.contentOfType(Proxy.CheckConnection.class)) {
			Proxy.CheckConnection request = m.message.getContentAs(Proxy.CheckConnection.class, gson);
			RpcResponse response = new RpcResponse();
			response.id = request.data;
			response.error = RmiError.UNREACHABLE;
			prependToEach(m.message.dests, src);
			handle(response, dest, m.message.dests);
		}
		else if (m.message.contentOfType(Proxy.OnConnectionClosed.class)) {
			Proxy.OnConnectionClosed request = m.message.getContentAs(Proxy.OnConnectionClosed.class, gson);
			Callback callback = new Callback();
			callback.target = dest;
			Parameter[] data = request.data.getValue(Parameter[].class, gson);
			callback.method = data[data.length-1].getValue(String.class, gson);
			callback.params = Arrays.copyOfRange(data, 0, data.length-1);
			RpcResponse response = new RpcResponse();
			response.error = RmiError.UNREACHABLE;
			prependToEach(m.message.dests, src);
			invokeCallback(callback, response, dest, m.message.dests);
		}
		else System.err.println("Unexpected delivery failure of " + m.message.contentType);
	}
	
	private void handle(Shutdown m) {
		interrupt();
		for (RpcHandler handler : handlers.values()) handler.shutdown();
	}
	
	private void handle(PeriodicCleanup m) {
		for (Iterator<Call> i=pendingCalls.values().iterator(); i.hasNext(); ) if (i.next().isExpired()) i.remove();
		for (RpcHandler h : handlers.values()) h.periodicCleanup();
	}
	
	private void prependToEach(List<Route> dests, Route src) {
		for (Route dest : dests) for (Iterator<URI> i=src.hops.descendingIterator(); i.hasNext(); ) dest.hops.addFirst(i.next());
	}
	
	private void invokeCallback(Callback callback, RpcResponse response, Route dest, List<Route> srcs) {
		URI targetUri = dest.hops.getFirst();
		RpcHandler handler = handlers.get(targetUri.getSchemeSpecificPart());
		if (handler != null) for (Route src : srcs) handler.handle(response, dest, src, callback);
		else System.err.println("Callback target not found " + targetUri);
	}
	
	@Override
	public void run() {
		try {
			while (true) process(mq.take());
		}
		catch (InterruptedException e) {
		}
	}
	
	private static class PeriodicCleanup {
	}
	
	public static class Options {
		public long cleanupInterval = 30*1000;
	}
}
