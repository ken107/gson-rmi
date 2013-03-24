package com.google.code.gsonrmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.annotations.Session;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.rmi.AbstractSession;
import com.google.code.gsonrmi.transport.rmi.Call;
import com.google.code.gsonrmi.transport.rmi.RmiService;
import com.google.code.gsonrmi.transport.tcp.TcpProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Test {
	
	private final Transport t;
	
	public Test(Transport t) {
		this.t = t;
	}
	
	@RMI
	public int aMethod(String name, @Session(create=true) AbstractSession session) throws Exception {
		//throw new NumberFormatException("Bad");
		System.out.println(session.id + " " + session.lastAccessed);
		return name.length();
	}
	
	@RMI
	public void shutdown(@Session AbstractSession session) {
		System.out.println(session.id + " " + session.lastAccessed);
		t.shutdown();
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Exception.class, new ExceptionSerializer())
				.registerTypeAdapter(Parameter.class, new ParameterSerializer()).create();
		
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30100)), t, gson).start();
		new RmiService(t, gson).start();
		
		new Call(new Route(new URI("rmi:service")), "register", "anObject", new Test(t)).send(t);
	}
}
