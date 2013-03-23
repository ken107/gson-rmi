package com.google.code.gsonrmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.code.gsonrmi.transport.rmi.RmiService;
import com.google.code.gsonrmi.transport.tcp.TcpProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestClient {
	
	private final Transport t;
	private final Gson gson;
	
	public TestClient(Transport t, Gson gson) {
		this.t = t;
		this.gson = gson;
	}

	@RMI
	public void returnValue(Integer value, RpcError error) {
		System.out.println(value + " " + error);
		if (error != null && error.code == -32000) error.data.getValue(Exception.class, gson).printStackTrace();
		t.shutdown();
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Exception.class, new ExceptionSerializer())
				.registerTypeAdapter(Parameter.class, new ParameterSerializer()).create();
		
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30101)), t, gson).start();
		new RmiService(t, gson).start();
		
		new Call(new Route(new URI("rmi:service")), "register", "anotherObject", new TestClient(t, gson)).send(t);
		
		Route r = new Route(new URI("tcp://localhost:30100"), new URI("rmi:anObject"));
		new Call(r, "aMethod", "Hello, World!").callback(new Route(new URI("rmi:anotherObject")), "returnValue").send(t);
		new Call(r, "shutdown").send(t);
		
		//TODO: custom invoker/dedicated invoker, session
	}
}
