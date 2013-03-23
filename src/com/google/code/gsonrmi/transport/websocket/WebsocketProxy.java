package com.google.code.gsonrmi.transport.websocket;

import com.google.code.gsonrmi.transport.Proxy;
import com.google.code.gsonrmi.transport.Transport;
import com.google.gson.Gson;

public class WebsocketProxy extends Proxy {

	public WebsocketProxy(Transport t, Gson serializer) {
		super(t, serializer);
	}

	@Override
	public String getScheme() {
		return "ws";
	}

	@Override
	public Connection createConnection(String authority) {
		return null;
	}

}
