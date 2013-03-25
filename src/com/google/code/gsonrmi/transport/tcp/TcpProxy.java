package com.google.code.gsonrmi.transport.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Proxy;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public class TcpProxy extends Proxy {
	
	private final List<TcpListener> listeners;
	
	public TcpProxy(List<InetSocketAddress> listeningAddresses, Transport transport, Gson serializer) throws IOException {
		super(transport, serializer);
		listeners = new LinkedList<TcpListener>();
		for (InetSocketAddress address : listeningAddresses) {
			TcpListener l = new TcpListener(address);
			l.start();
			listeners.add(l);
		}
	}

	@Override
	protected void handle(Shutdown m) {
		super.handle(m);
		for (TcpListener l : listeners) l.shutdown();
	}

	@Override
	public String getScheme() {
		return "tcp";
	}

	@Override
	public Connection createConnection(String remoteAuthority) {
		try {
			String[] tokens = remoteAuthority.split(":");
			Socket s = new Socket(tokens[0], Integer.parseInt(tokens[1]));
			TcpConnection c = new TcpConnection(s, remoteAuthority);
			c.start();
			return c;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
		catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private class TcpListener extends Thread {
		private final ServerSocket ss;

		public TcpListener(InetSocketAddress address) throws IOException {
			ss = new ServerSocket();
			ss.bind(address);
		}
		
		public void shutdown() {
			try {
				ss.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					Socket s = ss.accept();
					InetSocketAddress addr = (InetSocketAddress) s.getRemoteSocketAddress();
					TcpConnection c = new TcpConnection(s, addr.getHostString() + ":" + addr.getPort());
					c.start();
					addConnection(c);
				}
			}
			catch (IOException e) {
				if (!ss.isClosed()) e.printStackTrace();
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class TcpConnection extends Thread implements Connection {
		private final Socket s;
		private final BufferedReader in;
		private final PrintWriter out;
		private final URI remoteAddr;
		
		public TcpConnection(Socket socket, String remoteAuthority) throws URISyntaxException, IOException {
			s = socket;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
			remoteAddr = new URI(getScheme(), remoteAuthority, null, null, null);
		}
		
		@Override
		public String getRemoteAuthority() {
			return remoteAddr.getAuthority();
		}
		
		@Override
		public void shutdown() {
			try {
				s.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void send(Message m) {
			LinkedList<Route> dests = new LinkedList<Route>();
			for (Route dest : m.dests) dests.add(new Route(dest));
			for (Route dest : dests) dest.hops.removeFirst();
			out.println(gson.toJson(new Message(m.src, dests, m.content, m.contentType)));
		}
		
		@Override
		public void run() {
			try {
				String line;
				while ((line = in.readLine()) != null) {
					Message m = gson.fromJson(line, Message.class);
					m.src.hops.addFirst(remoteAddr);
					transport.send(m);
				}
			}
			catch (IOException e) {
				if (!s.isClosed()) e.printStackTrace();
			}
		}
	}
}
