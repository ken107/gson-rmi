package com.google.code.gsonrmi.transport.rmi;

import java.util.function.BiConsumer;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.transport.Route;

public class Callback {

	public Route target;
	public String method;
	public Parameter[] params;
	public AbstractSession session;
	public BiConsumer<Parameter, RpcError> consumer;
}
