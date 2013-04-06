package com.google.code.gsonrmi.transport.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.MessageProcessor;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public class HttpClient extends MessageProcessor {

	private final Transport t;
	private final Gson gson;
	private final Executor exec;
	
	public HttpClient(Transport transport, Gson serializer, Executor executor) {
		t = transport;
		gson = serializer;
		exec = executor;
	}
	
	@Override
	protected void process(Message m) {
		if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else exec.execute(new Task(m));
	}

	private void handle(Shutdown m) {
		if (exec instanceof ExecutorService) ((ExecutorService) exec).shutdown();
	}

	private class Task implements Runnable {
		private Message m;
		
		public Task(Message message) {
			m = message;
		}
		
		@Override
		public void run() {
		for (Route dest : m.dests) {
			RpcResponse response;
			try {
				URL requestUrl = dest.hops[0].toURL();
				HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json");
				con.setRequestProperty("Content-Class", m.contentType);
				con.setDoOutput(true);
				con.getOutputStream().write(m.content.getSerializedValue(gson).toString().getBytes("utf-8"));
				
				int responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					response = gson.fromJson(new InputStreamReader(con.getInputStream(), "utf-8"), RpcResponse.class);
				}
				else {
					RpcRequest request = m.content.getValue(RpcRequest.class, gson);
					response = new RpcResponse();
					response.id = request.id;
					response.error = new RpcError(HttpError.HTTP_REQUEST_FAILED, responseCode + " " + con.getResponseMessage());
				}
			}
			catch (IOException e) {
				RpcRequest request = m.content.getValue(RpcRequest.class, gson);
				response = new RpcResponse();
				response.id = request.id;
				response.error = new RpcError(HttpError.IO_EXCEPTION, e);
			}
			t.send(new Message(dest, Arrays.asList(m.src), response));
		}
		}
	}
}
