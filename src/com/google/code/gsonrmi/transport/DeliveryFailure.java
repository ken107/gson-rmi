package com.google.code.gsonrmi.transport;

public class DeliveryFailure {

	public final Message message;
	
	public DeliveryFailure(Message m) {
		message = m;
	}
}
