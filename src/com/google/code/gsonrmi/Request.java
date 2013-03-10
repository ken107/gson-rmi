package com.google.code.gsonrmi;

import java.net.URI;

public class Request {

	public URI requestURI;
	public String method;
	public Parameter[] params;
	public Parameter id;
	public Parameter context;
}
