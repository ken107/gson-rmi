package com.google.code.gsonrmi.transport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.code.gsonrmi.transport.Transport.DeliveryFailure;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public abstract class Proxy extends Thread {

	private final BlockingQueue<Message> mq;
	protected final Transport transport;
	protected final Gson gson;
	private final Map<String, Connection> cons;
	
	protected Proxy(Transport t, Gson serializer) {
		mq = new LinkedBlockingQueue<Message>();
		transport = t;
		transport.register(getScheme(), mq);
		gson = serializer;
		cons = new HashMap<String, Connection>();
	}
	
	protected abstract String getScheme();
	protected abstract Connection createConnection(String remoteAuthority);
	
	protected void addConnection(Connection c) {
		mq.add(new Message(null, null, new AddConnection(c)));
	}
	
	@Override
	public void run() {
		try {
			while (true) process(mq.take());
		}
		catch (InterruptedException e) {
		}
	}
	
	protected void process(Message m) {
		if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else if (m.contentOfType(AddConnection.class)) handle(m.getContentAs(AddConnection.class, gson));
		else handle(m);
	}
	
	protected void handle(Shutdown m) {
		interrupt();
		for (Connection c : cons.values()) c.shutdown();
	}
	
	private void handle(AddConnection m) {
		cons.put(m.con.getRemoteAuthority(), m.con);
	}
	
	protected void handle(Message m) {
		List<Route> failedRoutes = new LinkedList<Route>();
		for (Map.Entry<String, List<Route>> entry : Collections.group(m.dests, Route.GroupBy.AUTHORITY).entrySet()) {
			String authority = entry.getKey();
			List<Route> dests = entry.getValue();
			Connection c = cons.get(authority);
			if (c == null || !c.isAlive()) {
				cons.remove(authority);
				c = createConnection(authority);
				if (c != null) cons.put(authority, c);
			}
			if (c != null) c.send(new Message(m.src, dests, m.content, m.contentType));
			else failedRoutes.addAll(dests);
		}
		if (!failedRoutes.isEmpty() && !m.contentOfType(DeliveryFailure.class)) {
			Object failure = new DeliveryFailure(new Message(m.src, failedRoutes, m.content, m.contentType));
			transport.send(new Message(null, Arrays.asList(m.src), failure));
		}
	}
	
	private static class AddConnection {
		public final Connection con;
		public AddConnection(Connection c) {
			con = c;
		}
	}
	
	public static interface Connection {
		String getRemoteAuthority();
		boolean isAlive();
		void send(Message m);
		void shutdown();
	}
}