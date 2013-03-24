package com.google.code.gsonrmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.code.gsonrmi.Invoker.ParamProcessor;
import com.google.code.gsonrmi.annotations.Injected;
import com.google.code.gsonrmi.annotations.ParamType;
import com.google.gson.Gson;

public class DefaultParamProcessor implements ParamProcessor {
	
	private final Gson paramDeserializer;
	
	public DefaultParamProcessor(Gson paramDeserializer) {
		this.paramDeserializer = paramDeserializer;
	}

	@Override
	public Object injectParam(Type paramType, Annotation[] paramAnnotations, Object context) throws ParamValidationException {
		return null;
	}

	@Override
	public Object processParam(Parameter param, Type paramType, Annotation[] paramAnnotations, Object context) throws ParamValidationException {
		if (param == null) return null;
		ParamType paramTypeAnnotation = findAnnotation(paramAnnotations, ParamType.class);
		if (paramTypeAnnotation != null) paramType = paramTypeAnnotation.value();
		return param.getValue(paramType, paramDeserializer);
	}

	@Override
	public boolean isInjectedParam(Annotation[] paramAnnotations) {
		for (Annotation paramAnnotation : paramAnnotations) {
			if (findAnnotation(paramAnnotation.annotationType().getAnnotations(), Injected.class) != null) return true;
		}
		return false;
	}
	
	protected <T> T findAnnotation(Annotation[] paramAnnotations, Class<T> type) {
		for (Annotation a : paramAnnotations) if (type.isInstance(a)) return type.cast(a);
		return null;
	}

}
