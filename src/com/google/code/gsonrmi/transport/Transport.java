package com.google.code.gsonrmi.transport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class Transport {

	private final Map<String, Queue<Message>> queues;
	private final Timer timer;
	
	public Transport() {
		queues = new HashMap<String, Queue<Message>>();
		timer = new Timer();
	}
	
	public void shutdown() {
		Message m = new Message(null, null, new Shutdown());
		for (Queue<Message> queue : queues.values()) queue.add(m);
		timer.cancel();
	}
	
	public void register(String scheme, Queue<Message> messageQueue) {
		queues.put(scheme, messageQueue);
	}
	
	public void send(Message m) {
		for (Map.Entry<String, List<Route>> entry : Collections.group(m.dests, Route.GroupBy.SCHEME).entrySet()) {
			Queue<? super Message> queue = queues.get(entry.getKey());
			if (queue != null) queue.add(new Message(m.src, entry.getValue(), m.content, m.contentType));
			else new RuntimeException("No handler for protocol " + entry.getKey()).printStackTrace();
		}
	}
	
	public TimerTask sendAfter(Message m, long delay) {
		TimerTask task = new SendTask(m);
		timer.schedule(task, delay);
		return task;
	}
	
	public TimerTask sendEvery(Message m, long delay, long period) {
		TimerTask task = new SendTask(m);
		timer.schedule(task, delay, period);
		return task;
	}
	
	private class SendTask extends TimerTask {
		private final Message message;
		private SendTask(Message m) {
			message = m;
		}
		@Override
		public void run() {
			send(message);
		}
	}
	
	public static class Shutdown {
	}
}
