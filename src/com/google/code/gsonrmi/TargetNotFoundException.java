package com.google.code.gsonrmi;

public class TargetNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public TargetNotFoundException() {
		
	}
	
	public TargetNotFoundException(String message) {
		super(message);
	}

}
