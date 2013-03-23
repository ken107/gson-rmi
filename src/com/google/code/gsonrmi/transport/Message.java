package com.google.code.gsonrmi.transport;

import java.util.LinkedList;
import java.util.List;

import com.google.code.gsonrmi.Parameter;
import com.google.gson.Gson;

public final class Message {

	public final Route src;
	public final List<Route> dests;
	public final Parameter content;
	public final String contentType;
	
	public Message(Route src, List<Route> dests, Parameter content, String contentType) {
		this.src = src != null ? src : new Route();
		this.dests = dests != null ? dests : new LinkedList<Route>();
		this.content = content;
		this.contentType = contentType;
	}
	
	public Message(Route src, List<Route> dests, Object content) {
		this(src, dests, new Parameter(content), content.getClass().getName());
	}
	
	public boolean contentOfType(Class<?> c) {
		return c.getName().equals(contentType);
	}
	
	public <T> T getContentAs(Class<T> c, Gson deserializer) {
		return content.getValue(c, deserializer);
	}
}
