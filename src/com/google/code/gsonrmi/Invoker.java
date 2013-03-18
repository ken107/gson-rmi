package com.google.code.gsonrmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.code.gsonrmi.annotations.RMI;
import com.google.gson.JsonParseException;

public class Invoker {

	private final ParamProcessor paramProcessor;
	
	public Invoker(ParamProcessor paramProcessor) {
		this.paramProcessor = paramProcessor;
	}
	
	public Response doInvoke(Request request, Object target, Object context) {
		Response response = new Response();
		response.id = request.id;
		try {
			Method m = findMethod(target, request.method, request.params);
			if (m == null) throw new NoSuchMethodException(request.method);
			
			Type[] paramTypes = m.getGenericParameterTypes();
			Annotation[][] paramAnnotations = m.getParameterAnnotations();
			Object[] processedParams = new Object[paramTypes.length];
			for (int i=0, j=0; i<processedParams.length; i++) {
				if (paramProcessor.isInjectedParam(paramAnnotations[i])) processedParams[i] = paramProcessor.injectParam(paramTypes[i], paramAnnotations[i], context);
				else processedParams[i] = paramProcessor.processParam(request.params[j++], paramTypes[i], paramAnnotations[i], context);
			}
			response.result = new Parameter(m.invoke(target, processedParams), m.getGenericReturnType());
		}
		catch (JsonParseException e) {
			response.error = new Error(-32700, "Parse error");
		}
		catch (InvocationTargetException e) {
			response.error = new Error(-32000, "Invocation exception", e.getCause());
		}
		catch (IllegalAccessException e) {
			response.error = new Error(-32601, "Method not found");
		}
		catch (IllegalArgumentException e) {
			response.error = new Error(-32602, "Invalid params");
		}
		catch (NoSuchMethodException e) {
			response.error = new Error(-32601, "Method not found");
		}
		return response;
	}
	
	public Method findMethod(Object target, String method, Parameter[] params) {
		for (Method m : target.getClass().getMethods()) {
			RMI rmi = m.getAnnotation(RMI.class);
			if (rmi != null) {
				String rmiName = rmi.value();
				if (rmiName.isEmpty()) rmiName = m.getName();
				if (rmiName.equals(method)) {
					int countInjects = 0;
					for (Annotation[] a : m.getParameterAnnotations()) if (paramProcessor.isInjectedParam(a)) countInjects++;
					if (params.length + countInjects == m.getParameterTypes().length) return m;
				}
			}
		}
		return null;
	}
	
	public interface ParamProcessor {
		boolean isInjectedParam(Annotation[] paramAnnotations);
		Object injectParam(Type paramType, Annotation[] paramAnnotations, Object context);
		Object processParam(Parameter param, Type paramType, Annotation[] paramAnnotations, Object context);
	}
}
