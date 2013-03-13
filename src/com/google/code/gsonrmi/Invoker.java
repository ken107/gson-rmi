package com.google.code.gsonrmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Invoker {

	private final MethodLocator methodLocator;
	private final ParamProcessor paramProcessor;
	
	public Invoker(MethodLocator methodLocator, ParamProcessor paramProcessor) {
		this.methodLocator = methodLocator;
		this.paramProcessor = paramProcessor;
	}
	
	public Response doInvoke(Request request, Object target) {
		Response response = new Response();
		response.id = request.id;
		response.context = request.context;
		
		try {
			if (target == null) throw new TargetNotFoundException(request.requestURI.toString());
			
			Method m = methodLocator.get(target, request.method, request.params);
			if (m == null) throw new NoSuchMethodException(request.method);
			
			Type[] paramTypes = m.getGenericParameterTypes();
			Annotation[][] paramAnnotations = m.getParameterAnnotations();
			Object[] processedParams = new Object[paramTypes.length];
			for (int i=0, j=0; i<processedParams.length; i++) {
				if (paramProcessor.isInjectedParam(paramAnnotations[i])) processedParams[i] = paramProcessor.injectParam(paramTypes[i], paramAnnotations[i], request.context);
				else processedParams[i] = paramProcessor.processParam(request.params[j++], paramTypes[i], paramAnnotations[i], request.context);
			}
			
			response.result = new Parameter(m.invoke(target, processedParams), m.getGenericReturnType());
		}
		catch (InvocationTargetException e) {
			response.error = new Parameter(e.getCause());
		}
		catch (Throwable e) {
			response.error = new Parameter(e);
		}
		
		return response;
	}
}
