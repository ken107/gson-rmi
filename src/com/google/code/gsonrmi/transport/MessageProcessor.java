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
		boolean quit = false;
		while (!quit) {
			try {
				Message m = mq.take();
				if (m.contentOfType(Transport.Shutdown.class)) quit = true;
				process(m);
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				quit = true;
			}
		}
	}
	
	protected abstract void process(Message m);
}
