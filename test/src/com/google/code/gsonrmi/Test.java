package com.google.code.gsonrmi;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.google.code.gsonrmi.annotations.RMI;
import com.google.code.gsonrmi.serializer.ExceptionSerializer;
import com.google.code.gsonrmi.serializer.ParameterSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Test {
	
	@RMI("someMethod")
	public List<String> aMethod(String firstName, String lastName) throws Exception {
		LinkedList<String> out = new LinkedList<String>();
		out.add("Hello, " + firstName + " " + lastName);
		out.add("Welcome to Gson RMI");
		throw new NumberFormatException("bad number format!");
		//return out;
	}
	
	public static void main(String[] args) throws URISyntaxException {
		//create the invoker
		Gson gson = new GsonBuilder().registerTypeAdapter(Parameter.class, new ParameterSerializer()).registerTypeAdapter(Exception.class, new ExceptionSerializer()).create();
		DefaultParamProcessor paramProcessor = new DefaultParamProcessor(gson);
		Invoker invoker = new Invoker(paramProcessor);
		
		//create a sample request
		Request r = new Request();
		r.method = "someMethod";
		r.params = new Parameter[] {
				new Parameter("John"),
				new Parameter((Object) null)
		};
		r.id = new Parameter(1);
		
		//test request serialization
		String json = gson.toJson(r);
		System.out.println(json);
		
		//test request deserialization
		r = gson.fromJson(json, Request.class);
		System.out.println(gson.toJson(r));
		
		//invoke on a test object
		Test target = new Test();
		Response s = invoker.doInvoke(r, target, null);
		
		//test response serialization
		json = gson.toJson(s);
		System.out.println(json);
		
		//test response deserialization
		s = gson.fromJson(json, Response.class);
		System.out.println(gson.toJson(s));
	}
}
