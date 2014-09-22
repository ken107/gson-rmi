package com.google.code.gsonrmi.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.google.code.gsonrmi.Parameter;
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

public class SampleServer {
	
	@RMI
	public String someMethod(String name) {
		return "Hello, " + name;
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		//setup the transport layer
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		Transport t = new Transport();
		new TcpProxy(Arrays.asList(new InetSocketAddress(30100)), t, gson).start();
		new RmiService(t, gson).start();

		//register an object for remote invocation
		new Call(new Route(new URI("rmi:service")), "register", "herObject", new SampleServer()).send(t);
	}
}
