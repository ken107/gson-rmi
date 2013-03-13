package com.google.code.gsonrmi;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.code.gsonrmi.annotations.Context;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Test {
	
	public List<String> aMethod(String firstName, String lastName, @Context Map<String, String> context) throws Exception {
		LinkedList<String> out = new LinkedList<String>();
		out.add("Hello, " + firstName + " " + lastName);
		out.add("Welcome to Gson RMI");
		context.put("Session-ID", UUID.randomUUID().toString());
		//throw new NumberFormatException("bad number format!");
		return out;
	}
	
	public static void main(String[] args) throws URISyntaxException {
		//create the invoker
		Gson gson = new GsonBuilder().registerTypeAdapter(Parameter.class, new ParameterSerializer()).registerTypeAdapter(Exception.class, new ExceptionSerializer()).create();
		ParamProcessor paramProcessor = new DefaultParamProcessor(gson);
		Invoker invoker = new Invoker(new DefaultMethodLocator(paramProcessor), paramProcessor);
		
		//create a sample request
		Request r = new Request();
		r.method = "aMethod";
		r.params = new Parameter[] {
				new Parameter("John"),
				new Parameter((Object) null)
		};
		r.id = new Parameter(1);
		r.context = new Parameter(new HashMap<String, String>());
		
		//test request serialization
		String json = gson.toJson(r);
		System.out.println(json);
		
		//test request deserialization
		r = gson.fromJson(json, Request.class);
		System.out.println(gson.toJson(r));
		
		//invoke on a test object
		Test target = new Test();
		Response s = invoker.doInvoke(r, target);
		
		//test response serialization
		json = gson.toJson(s);
		System.out.println(json);
		
		//test response deserialization
		s = gson.fromJson(json, Response.class);
		System.out.println(gson.toJson(s));
	}
}
