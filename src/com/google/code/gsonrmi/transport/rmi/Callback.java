package com.google.code.gsonrmi.transport.rmi;

import java.net.URI;

import com.google.code.gsonrmi.Parameter;

public class Callback {

	public URI target;
	public String method;
	public Parameter[] params;
	public AbstractSession session;
}
