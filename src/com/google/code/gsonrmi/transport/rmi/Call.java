package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
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
	long timeSent;
	public static int defaultExpireSec = 60;
	public int expireSec = defaultExpireSec;
	
	public Call(Route target, String method, Object... args) {
		this(Arrays.asList(target), method, args);
	}
	
	public Call(List<Route> targets, String method, Object... args) {
		this.targets = targets;
		this.method = method;
		this.params = toParams(args);
	}
	
	public Call expire(int sec) {
		expireSec = sec;
		return this;
	}
	
	public boolean isExpired() {
		return expireSec > 0 && System.currentTimeMillis()-timeSent > expireSec*1000;
	}
	
	public Call callback(URI target, String method, Object... params) {
		return callback(new Route(target), method, params);
	}
	
	public Call callback(Route target, String method, Object... args) {
		callback = new Callback();
		callback.target = target;
		callback.method = method;
		callback.params = toParams(args);
		return this;
	}
	
	public Call session(AbstractSession session) {
		if (callback == null) throw new RuntimeException("Callback must be set before session");
		if (session.id == null) session.id = UUID.randomUUID().toString();
		try {
			callback.session = session;
			callback.target.hops[0] = new URI(callback.target.hops[0].getScheme(), callback.target.hops[0].getSchemeSpecificPart(), session.id);
			return this;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void send(Transport t) {
		t.send(getMessage());
	}
	
	public TimerTask sendAfter(Transport t, long delay) {
		return t.sendAfter(getMessage(), delay);
	}
	
	public TimerTask sendEvery(Transport t, long delay, long period) {
		return t.sendEvery(getMessage(), delay, period);
	}
	
	private Message getMessage() {
		return new Message(null, Arrays.asList(new Route(URI.create("rmi:service"))), this);
	}
	
	private Parameter[] toParams(Object[] args) {
		Parameter[] params = new Parameter[args.length];
		for (int i=0; i<args.length; i++) {
			if (args[i] != null) params[i] = args[i] instanceof Parameter ? (Parameter) args[i] : new Parameter(args[i]);
		}
		return params;
	}
}
