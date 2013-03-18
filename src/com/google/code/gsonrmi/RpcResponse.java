package com.google.code.gsonrmi;

public class RpcResponse {

	public String jsonrpc = "2.0";
	public Parameter result;
	public RpcError error;
	public Parameter id;
}
