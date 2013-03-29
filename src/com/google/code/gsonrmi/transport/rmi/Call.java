package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;

public class Call {

	public final List<Route> targets;
	public final String method;
	public final Parameter[] params;
	public Callback callback;
	public long timeSent;
	
	public Call(URI target, String method, Object... params) {
		this(new Route(target), method, params);
	}
	
	public Call(Route target, String method, Object... params) {
		this(Arrays.asList(target), method, params);
	}
	
	public Call(List<Route> targets, String method, Object... params) {
		this.targets = targets;
		this.method = method;
		this.params = new Parameter[params.length];
		for (int i=0; i<params.length; i++) this.params[i] = params[i] != null ? new Parameter(params[i]) : null;
	}
	
	public Call callback(URI target, String method, Object... params) {
		return callback(new Route(target), method, params);
	}
	
	public Call callback(Route target, String method, Object... params) {
		callback = new Callback();
		callback.target = target;
		callback.method = method;
		callback.params = new Parameter[params.length];
		for (int i=0; i<params.length; i++) callback.params[i] = params[i] != null ? new Parameter(params[i]) : null;
		return this;
	}
	
	public Call session(AbstractSession session) {
		if (callback == null) throw new RuntimeException("Callback must be set before session");
		if (session.id == null) session.id = UUID.randomUUID().toString();
		try {
			callback.session = session;
			URI targetUri = callback.target.hops.removeFirst();
			callback.target.hops.addFirst(new URI(targetUri.getScheme(), targetUri.getSchemeSpecificPart(), session.id));
			return this;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void send(Transport t) {
		t.send(getMessage());
	}
	
	public void sendAfter(Transport t, long delay) {
		t.sendAfter(getMessage(), delay);
	}
	
	public void sendEvery(Transport t, long delay, long period) {
		t.sendEvery(getMessage(), delay, period);
	}
	
	private Message getMessage() {
		return new Message(null, Arrays.asList(new Route(URI.create("rmi:service"))), this);
	}
}
