package com.google.code.gsonrmi.server;

import java.io.*;
import java.net.*;
import com.google.gson.*;
import com.google.code.gsonrmi.*;

public class RpcServlet extends Thread {

	private Socket s;
	private RpcTarget target;
	private Gson gson;
	private JsonStreamParser in;
	private Writer out;
	

	public RpcServlet(Socket s, RpcTarget target, Gson gson) throws IOException {
		this.s = s;
		this.target = target;
		this.gson = gson;
		in = new JsonStreamParser(new InputStreamReader(s.getInputStream(), "utf-8"));
		out = new OutputStreamWriter(s.getOutputStream(), "utf-8");
		setDaemon(true);
	}
	
	@Override
	public void run() {
		try {
			while (in.hasNext()) {
				RpcRequest request = gson.fromJson(in.next(), RpcRequest.class);
				RpcResponse response = target.doInvoke(request);
				out.write(gson.toJson(response));
				out.flush();
			}
		}
		catch (IOException e) {
			System.err.println("Error writing response to stream");
			e.printStackTrace();
		}
		catch (JsonParseException e) {
			System.err.println("Error parsing incoming JSON message");
			e.printStackTrace();
		}
		finally {
			try {
				s.close();
			}
			catch (IOException e) {
			}
		}
	}

}