package com.google.code.gsonrmi.transport;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public abstract class Proxy extends MessageProcessor {

	protected final Transport transport;
	protected final Gson gson;
	private final Map<String, Connection> cons;
	private final TimerTask cleanupTask;
	
	protected Proxy(Transport t, Gson serializer) {
		this(t, serializer, null);
	}
	
	protected Proxy(Transport t, Gson serializer, Options options) {
		if (options == null) options = new Options();
		transport = t;
		transport.register(getScheme(), mq);
		gson = serializer;
		cons = new HashMap<String, Connection>();
		cleanupTask = t.sendEvery(new Message(null, Arrays.asList(new Route(URI.create(getScheme() + ":proxy"))), new CleanUp()), options.cleanupInterval, options.cleanupInterval);
	}
	
	protected abstract String getScheme();
	protected abstract Connection createConnection(String remoteAuthority);
	
	public void addConnection(Connection c) {
		mq.add(new Message(null, null, new AddConnection(c)));
	}
	
	protected Collection<Connection> getConnections() {
		return cons.values();
	}
	
	@Override
	protected void process(Message m) {
		if (m.contentOfType(Shutdown.class)) handle(m.getContentAs(Shutdown.class, gson));
		else if (m.contentOfType(AddConnection.class)) handle(m.getContentAs(AddConnection.class, gson));
		else if (m.contentOfType(CleanUp.class)) handle(m.getContentAs(CleanUp.class, gson));
		else handle(m);
	}
	
	protected void handle(Shutdown m) {
		for (Connection c : cons.values()) c.shutdown();
		cleanupTask.cancel();
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
	
	protected void handle(CleanUp m) {
		int count = cons.size();
		for (Iterator<Connection> i=cons.values().iterator(); i.hasNext(); ) if (!i.next().isAlive()) i.remove();
		if (cons.size() < count) System.err.println("INFO: " + getClass().getSimpleName() + " cleanup connections " + count + " -> " + cons.size());
	}
	
	protected static class CleanUp {
	}
	
	public static class Options {
		public long cleanupInterval = 60*1000;
	}
	
	public static class AddConnection {
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
	
	/**
	 * Access proxies allow external clients to access network services.
	 * These connections are normally terminal and short-lived.
	 * Access proxies should not forward this message to clients.
	 * Sender of this message can rely on DeliveryFailure to know that
	 * a client is no longer reachable.
	 */
	public static class CheckConnection {
		public Parameter data;
	}
	
	/**
	 * Access proxies allow external clients to access network services.
	 * These connections are normally terminal and short-lived.
	 * Access proxies should save this message and send DeliveryFailure
	 * to let the sender know when the connection closes.
	 */
	public static class OnConnectionClosed {
		public Parameter data;
	}
}
