package com.google.code.gsonrmi.transport.rmi;

import com.google.code.gsonrmi.ParamValidationException;

public class InvalidSessionException extends ParamValidationException {

	private static final long serialVersionUID = 1L;

	public InvalidSessionException() {
	}
	
	public InvalidSessionException(String message) {
		super(message);
	}
}
