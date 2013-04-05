package com.google.code.gsonrmi.transport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class MessageProcessor extends Thread {

	protected final BlockingQueue<Message> mq;
	
	protected MessageProcessor() {
		mq = new LinkedBlockingQueue<Message>();
	}
	
	@Override
	public void run() {
		try {
			Message m;
			do process(m = mq.take());
			while (!m.contentOfType(Transport.Shutdown.class));
		}
		catch (InterruptedException e) {
		}
	}
	
	protected abstract void process(Message m);
}
