package com.google.code.gsonrmi;

public class Request {

	public String jsonrpc = "2.0";
	public String method;
	public Parameter[] params;
	public Parameter id;
}
