package com.google.code.gsonrmi;

public class ParamValidationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ParamValidationException() {
	}
	
	public ParamValidationException(String message) {
		super(message);
	}

}
