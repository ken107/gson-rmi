package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.code.gsonrmi.DefaultParamProcessor;
import com.google.code.gsonrmi.Invoker;
import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.transport.Message;
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
	private final Map<String, Object> objs;
	private final Gson gson;
	private final Invoker invoker;
	private final HashMap<Integer, Call> callbacks;
	private int idGen;
	
	public RmiService(Transport transport, Gson deserializer) throws URISyntaxException {
		addr = new URI(SCHEME, "service", null);
		mq = new LinkedBlockingQueue<Message>();
		t = transport;
		t.register(SCHEME, mq);
		objs = new HashMap<String, Object>();
		objs.put("service", this);
		gson = deserializer;
		invoker = new Invoker(new DefaultParamProcessor(gson));
		callbacks = new HashMap<Integer, Call>();
	}
	
	@RMI
	public URI register(String id, Object o) throws URISyntaxException {
		objs.put(id, o);
		return new URI(SCHEME, id, null);
	}
	
	private void process(Message m) {
		if (m.contentOfType(Call.class)) handle(m.getContentAs(Call.class, gson));
		else if (m.contentOfType(RpcRequest.class)) handle(m.getContentAs(RpcRequest.class, gson), m);
		else if (m.contentOfType(RpcResponse.class)) handle(m.getContentAs(RpcResponse.class, gson));
		else if (m.contentOfType(DeliveryFailure.class)) handle(m.getContentAs(DeliveryFailure.class, gson));
		else if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else System.err.println("Unhandled message type: " + m.contentType);
	}
	
	private void handle(Call m) {
		Integer id = null;
		if (m.callback != null) {
			id = ++idGen;
			callbacks.put(id, m.callback);
		}
		RpcRequest request = new RpcRequest();
		request.method = m.method;
		request.params = m.params;
		request.id = id == null ? null : new Parameter(id);
		t.send(new Message(new Route(addr), m.targets, request));
	}
	
	private void handle(RpcRequest request, Message m) {
		String targetId = getTargetId(m.dests.get(0));
		Object target = targetId != null ? objs.get(targetId) : null;
		RpcResponse response;
		if (target != null) {
			response = invoker.doInvoke(request, target, null);
		}
		else {
			response = new RpcResponse();
			response.id = request.id;
			response.error = new RpcError(-32001, "Target not found with id " + targetId);
		}
		t.send(new Message(null, Arrays.asList(m.src), response));
	}
	
	private void handle(RpcResponse response) {
		if (response.id != null) {
			Integer responseId = response.id.getValue(Integer.class, gson);
			Call callback = callbacks.get(responseId);
			if (callback != null) {
				String targetId = getTargetId(callback.targets.get(0));
				Object target = targetId != null ? objs.get(targetId) : null;
				if (target != null) {
					RpcRequest request = new RpcRequest();
					request.method = callback.method;
					request.params = Arrays.copyOf(callback.params, callback.params.length+2);
					request.params[request.params.length-2] = response.result;
					request.params[request.params.length-1] = new Parameter(response.error);
					RpcResponse r = invoker.doInvoke(request, target, null);
					if (r.error != null) {
						System.err.println("Invoke response failed: " + r.error);
						if (r.error.code == -32000) r.error.data.getValue(Exception.class, gson).printStackTrace();
					}
				}
				else System.err.println("Target not found with id " + targetId);
			}
			else System.err.println("Callback missing with id " + responseId);
		}
		else if (response.error != null) {
			System.err.println("Unhandled failure response: " + response.error);
			if (response.error.code == -32000) response.error.data.getValue(Exception.class, gson).printStackTrace();
		}
	}
	
	private void handle(DeliveryFailure m) {
		if (m.message.contentOfType(RpcRequest.class)) {
			RpcRequest request = m.message.getContentAs(RpcRequest.class, gson);
			RpcResponse response = new RpcResponse();
			response.id = request.id;
			response.error = new RpcError(-32002, "Unreachable");
			handle(response);
		}
		else if (m.message.contentOfType(RpcResponse.class)) {
			RpcResponse response = m.message.getContentAs(RpcResponse.class, gson);
			Integer responseId = response.id.getValue(Integer.class, gson);
			System.err.println("Delivery failed for response with id " + responseId);
		}
		else System.err.println("Unexpected delivery failure of " + m.message.contentType);
	}
	
	private void handle(Shutdown m) {
		interrupt();
	}
	
	private String getTargetId(Route dest) {
		return dest.hops.getFirst().getSchemeSpecificPart();
	}
	
	@Override
	public void run() {
		try {
			while (true) process(mq.take());
		}
		catch (InterruptedException e) {
		}
	}
}
