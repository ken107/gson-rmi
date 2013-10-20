package com.google.code.gsonrmi.transport.websocket;

import com.google.code.gsonrmi.transport.Proxy;
import com.google.code.gsonrmi.transport.Transport;
import com.google.gson.Gson;

public class WebsocketProxy extends Proxy {

	public WebsocketProxy(Transport t, Gson serializer) {
		this(t, serializer, null);
	}
	
	public WebsocketProxy(Transport t, Gson serializer, Options options) {
		super(t, serializer, options);
	}

	@Override
	public String getScheme() {
		return "wsa";
	}

	@Override
	public Connection createConnection(String authority) {
		return null;
	}

}
