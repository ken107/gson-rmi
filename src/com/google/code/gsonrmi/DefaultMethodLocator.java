package com.google.code.gsonrmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class DefaultMethodLocator implements MethodLocator {
	
	private final ParamProcessor paramProcessor;
	
	public DefaultMethodLocator(ParamProcessor paramProcessor) {
		this.paramProcessor = paramProcessor;
	}

	@Override
	public Method get(Object target, String method, Parameter[] params) {
		for (Method m : target.getClass().getMethods()) {
			if (m.getName().equals(method)) {
				int countInjects = 0;
				for (Annotation[] a : m.getParameterAnnotations()) if (paramProcessor.isInjectedParam(a)) countInjects++;
				if (params.length + countInjects == m.getParameterTypes().length) return m;
			}
		}
		return null;
	}

}
