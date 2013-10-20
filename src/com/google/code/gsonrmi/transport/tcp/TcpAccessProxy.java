package com.google.code.gsonrmi.transport.tcp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.code.gsonrmi.transport.DeliveryFailure;
import com.google.code.gsonrmi.transport.Message;
import com.google.code.gsonrmi.transport.Proxy;
import com.google.code.gsonrmi.transport.Route;
import com.google.code.gsonrmi.transport.Transport;
import com.google.code.gsonrmi.transport.Transport.Shutdown;
import com.google.gson.Gson;

public class TcpAccessProxy extends Proxy {
	
	private final Listener listener;
	private final Options opts;
	
	public TcpAccessProxy(List<InetSocketAddress> listeningAddresses, Transport transport, Gson serializer) throws IOException {
		this(listeningAddresses, transport, serializer, null);
	}
	
	public TcpAccessProxy(List<InetSocketAddress> listeningAddresses, Transport transport, Gson serializer, Options options) throws IOException {
		super(transport, serializer, options);
		listener = new Listener(listeningAddresses);
		listener.start();
		opts = options;
	}

	@Override
	protected String getScheme() {
		return "tcpa";
	}

	@Override
	protected Connection createConnection(String remoteAuthority) {
		return null;
	}
	
	@Override
	protected void handle(Shutdown m) {
		super.handle(m);
		listener.shutdown();
	}
	
	protected void setSocketOptions(SocketChannel sc) throws IOException {
		sc.setOption(StandardSocketOptions.SO_KEEPALIVE, opts.keepAlive);
	}

	private class Listener extends Thread {
		private final Selector selector;
		private boolean shutdown;
		
		public Listener(List<InetSocketAddress> addresses) throws IOException {
			selector = Selector.open();
			for (InetSocketAddress address : addresses) {
				ServerSocketChannel channel = ServerSocketChannel.open();
				channel.configureBlocking(false);
				channel.socket().bind(address);
				channel.register(selector, SelectionKey.OP_ACCEPT);
			}
		}
		
		public void shutdown() {
			shutdown = true;
			selector.wakeup();
		}

		@Override
		public void run() {
			try {
				while (!shutdown) {
					selector.select();
					Set<SelectionKey> readyKeys = selector.selectedKeys();
					Iterator<SelectionKey> readyItor = readyKeys.iterator();
					while (readyItor.hasNext()) {
						SelectionKey key = readyItor.next();
						readyItor.remove();
						if (key.isValid()) {
							if (key.isAcceptable()) {
								SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
								sc.configureBlocking(false);
								setSocketOptions(sc);
								SelectionKey k = sc.register(selector, SelectionKey.OP_READ);
								InetSocketAddress addr = (InetSocketAddress) sc.getRemoteAddress();
								Connection con = new AccessConnection(k, new URI(getScheme(), addr.getHostString() + ":" + addr.getPort(), null, "a=1", null));
								addConnection(con);
								k.attach(con);
							}
							else if (key.isReadable()) ((AccessConnection) key.attachment()).read();
							else if (key.isWritable()) ((AccessConnection) key.attachment()).write();
						}
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
			try {
				selector.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class AccessConnection implements Connection {
		private final SelectionKey key;
		private final URI remoteAddr;
		private final ByteBuffer readBuffer;
		private final ByteBuffer writeBuffer;
		private final Queue<ByteBuffer> sendQueue;
		private final List<Message> onConnectionClose;
		
		public AccessConnection(SelectionKey selectionKey, URI remoteAddress) {
			key = selectionKey;
			remoteAddr = remoteAddress;
			readBuffer = ByteBuffer.allocate(opts.readBufferSize);
			writeBuffer = ByteBuffer.allocate(opts.writeBufferSize);
			sendQueue = new ConcurrentLinkedQueue<ByteBuffer>();
			onConnectionClose = new LinkedList<Message>();
		}
		
		public void read() {
			SocketChannel sc = (SocketChannel) key.channel();
			try {
				if (!readBuffer.hasRemaining()) throw new IOException("Line too long");
				int tail = readBuffer.position();
				int bytes = sc.read(readBuffer);
				if (bytes > 0) {
					byte[] b = readBuffer.array();
					readBuffer.flip();
					for (int i=tail; i<tail+bytes; i++) {
						if (b[i] == 10) {
							String line = new String(b, readBuffer.position(), i-readBuffer.position());
							Message m = gson.fromJson(line, Message.class);
							if (m != null) transport.send(new Message(m.src.addFirst(remoteAddr), m.dests, m.content, m.contentType));
							readBuffer.position(i+1);
						}
					}
					readBuffer.compact();
				}
				else if (bytes == -1) {
					sc.close();
					onClose();
				}
				else throw new IOException("Unexpected " + bytes + " bytes read");
			}
			catch (Exception e) {
				e.printStackTrace();
				try {
					sc.close();
					onClose();
				}
				catch (IOException ee) {
				}
			}
		}
		
		public void write() {
			SocketChannel sc = (SocketChannel) key.channel();
			while (writeBuffer.hasRemaining() && !sendQueue.isEmpty()) {
				ByteBuffer head = sendQueue.peek();
				if (head.remaining() <= writeBuffer.remaining()) {
					writeBuffer.put(head);
					sendQueue.remove();
				}
				else {
					head.limit(head.position() + writeBuffer.remaining());
					writeBuffer.put(head.slice());
					head.position(head.limit());
					head.limit(head.capacity());
				}
			}
			try {
				writeBuffer.flip();
				sc.write(writeBuffer);
				writeBuffer.compact();
				if (writeBuffer.position() == 0 && sendQueue.isEmpty()) key.interestOps(key.interestOps() &~ SelectionKey.OP_WRITE);
			}
			catch (IOException e) {
				e.printStackTrace();
				try {
					sc.close();
					onClose();
				}
				catch (IOException ee) {
				}
			}
		}

		@Override
		public String getRemoteAuthority() {
			return remoteAddr.getAuthority();
		}

		@Override
		public boolean isAlive() {
			return ((SocketChannel) key.channel()).isConnected();
		}

		@Override
		public void send(Message m) {
			if (m.contentOfType(CheckConnection.class));
			else if (m.contentOfType(OnConnectionClosed.class)) onConnectionClose.add(m);
			else {
				try {
					List<Route> dests = new LinkedList<Route>();
					for (Route dest : m.dests) dests.add(dest.removeFirst());
					String text = gson.toJson(new Message(m.src, dests, m.content, m.contentType)) + "\n";
					sendQueue.add(ByteBuffer.wrap(text.getBytes("utf-8")));
					key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
					key.selector().wakeup();
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				catch (CancelledKeyException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void shutdown() {
			try {
				((SocketChannel) key.channel()).close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void onClose() {
			for (Message m : onConnectionClose) transport.send(new Message(null, Arrays.asList(m.src), new DeliveryFailure(m)));
		}
	}
	
	public static class Options extends Proxy.Options {
		public int readBufferSize = 4096;
		public int writeBufferSize = 4096;
		public boolean keepAlive = true;
	}
}
