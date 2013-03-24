package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;

public class Call {

	public final List<Route> targets;
	public final String method;
	public final Parameter[] params;
	public Call callback;
	public long timeSent;
	
	public Call(Route target, String method, Object... params) {
		this(Arrays.asList(target), method, params);
	}
	
	public Call(List<Route> targets, String method, Object... params) {
		this.targets = new LinkedList<Route>();
		for (Route t : targets) this.targets.add(new Route(t));
		this.method = method;
		this.params = new Parameter[params.length];
		for (int i=0; i<params.length; i++) this.params[i] = params[i] != null ? new Parameter(params[i]) : null;
	}
	
	public Call callback(Route target, String method, Object... params) {
		callback = new Call(Arrays.asList(target), method, params);
		return this;
	}
	
	public void send(Transport t) {
		t.send(new Message(null, Arrays.asList(new Route(URI.create("rmi:service"))), this));
	}
}
