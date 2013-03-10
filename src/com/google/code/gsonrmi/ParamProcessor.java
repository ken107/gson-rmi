package com.google.code.gsonrmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface ParamProcessor {

	boolean isInjectedParam(Annotation[] paramAnnotations);
	Object injectParam(Type paramType, Annotation[] paramAnnotations, Parameter requestContext);
	Object processParam(Parameter param, Type paramType, Annotation[] paramAnnotations, Parameter requestContext);
}
