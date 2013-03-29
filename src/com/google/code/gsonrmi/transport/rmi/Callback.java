package com.google.code.gsonrmi.transport.rmi;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Route;

public class Callback {

	public Route target;
	public String method;
	public Parameter[] params;
	public AbstractSession session;
}
