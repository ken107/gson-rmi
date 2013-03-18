package com.google.code.gsonrmi;

public class RpcRequest {

	public String jsonrpc = "2.0";
	public String method;
	public Parameter[] params;
	public Parameter id;
}
