package com.google.code.gsonrmi.server;

import java.io.*;
import com.google.gson.*;
import com.google.code.gsonrmi.*;
import com.google.code.gsonrmi.annotations.*;
import com.google.code.gsonrmi.serializer.*;

public class SampleServer {

	@RMI
	public String someMethod1(String name) {
		return "Hello, " + name;
	}

	public static void main(String[] args) throws IOException {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();
		
		new RpcServer(30100, new RpcTarget(new SampleServer(), gson), gson).start();
	}
}
