package com.google.code.gsonrmi.server;

import java.io.*;
import java.net.*;
import com.google.gson.*;
import com.google.code.gsonrmi.*;
import com.google.code.gsonrmi.serializer.*;

public class SampleClient {

	public static void main(String[] args) throws IOException {
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Parameter.class, new ParameterSerializer())
			.create();

		Socket s = new Socket("localhost", 30100);
		Reader in = new InputStreamReader(s.getInputStream(), "utf-8");
		Writer out = new OutputStreamWriter(s.getOutputStream(), "utf-8");

		//send first req
		RpcRequest req = new RpcRequest();
		req.method = "someMethod1";
		req.params = new Parameter[] {new Parameter("Jack")};
		req.id = new Parameter(1);
		out.write(gson.toJson(req));

		//send second req
		req.params = new Parameter[] {new Parameter("Obama")};
		req.id = new Parameter(2);
		out.write(gson.toJson(req));
		out.flush();

		//print out all responses from the server
		int c;
		while ((c = in.read()) != -1) System.out.print((char)c);
		System.out.println();
		s.close();
	}
}
