package com.google.code.gsonrmi.transport.rmi;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.code.gsonrmi.DefaultParamProcessor;
import com.google.code.gsonrmi.Invoker;
import com.google.code.gsonrmi.ParamValidationException;
import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.RpcError;
import com.google.code.gsonrmi.RpcRequest;
import com.google.code.gsonrmi.RpcResponse;
import com.google.code.gsonrmi.annotations.ParamType;
import com.google.code.gsonrmi.annotations.Session;
import com.google.code.gsonrmi.annotations.Src;
import com.google.code.gsonrmi.transport.Route;
import com.google.gson.Gson;

public class DefaultRpcHandler implements RpcHandler {
	
	public static final int SESSION_EXPIRY = 30*60*1000;
	
	private final Object target;
	private final Invoker invoker;
	private final Map<String, AbstractSession> sessions;
	
	public DefaultRpcHandler(Object target, Gson paramDeserializer) {
		this.target = target;
		invoker = new Invoker(new CustomParamProcessor(paramDeserializer));
		sessions = new HashMap<String, AbstractSession>();
	}

	@Override
	public RpcResponse handle(RpcRequest request, URI dest, Route src) {
		return invoker.doInvoke(request, target, new Context(src, dest.getFragment()));
	}

	@Override
	public void handle(RpcResponse response, Call callback) {
		RpcRequest request = new RpcRequest();
		request.method = callback.method;
		request.params = Arrays.copyOf(callback.params, callback.params.length+2);
		request.params[request.params.length-2] = response.result;
		request.params[request.params.length-1] = new Parameter(response.error);
		
		RpcResponse r = invoker.doInvoke(request, target, new Context(null, callback.targets.get(0).hops.getFirst().getFragment()));
		if (r.error != null) {
			System.err.println("Invoke response failed:  method " + callback.method + ", " + r.error);
			if (r.error.equals(RpcError.INVOCATION_EXCEPTION)) r.error.data.getValue(Exception.class, null).printStackTrace();
		}
	}
	
	@Override
	public void shutdown() {
	}

	@Override
	public void periodicCleanup() {
		for (Iterator<AbstractSession> i=sessions.values().iterator(); i.hasNext(); ) {
			AbstractSession session = i.next();
			if (System.currentTimeMillis()-session.lastAccessed > SESSION_EXPIRY) i.remove();
		}
	}
	
	private class Context {
		public final Route src;
		public final String sessionId;
		
		public Context(Route src, String sessionId) {
			this.src = src;
			this.sessionId = sessionId;
		}
		
		public AbstractSession getSession(Type type, boolean create) {
			AbstractSession session = null;
			if (sessionId != null) {
				session = sessions.get(sessionId);
				if (session == null && create) {
					try {
						if (type instanceof ParameterizedType) type = ((ParameterizedType) type).getRawType();
						sessions.put(sessionId, session = (AbstractSession) ((Class<?>) type).newInstance());
						session.id = sessionId;
					}
					catch (ClassCastException e) {
						e.printStackTrace();
					}
					catch (InstantiationException e) {
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			if (session != null) session.lastAccessed = System.currentTimeMillis();
			return session;
		}
	}
	
	private static class CustomParamProcessor extends DefaultParamProcessor {
		public CustomParamProcessor(Gson paramDeserializer) {
			super(paramDeserializer);
		}
		
		@Override
		public Object injectParam(Type paramType, Annotation[] paramAnnotations, Object context) throws ParamValidationException {
			Context c = (Context) context;
			Session sessionAnn = findAnnotation(paramAnnotations, Session.class);
			if (sessionAnn != null) {
				ParamType paramTypeAnn = findAnnotation(paramAnnotations, ParamType.class);
				AbstractSession session = c.getSession(paramTypeAnn != null ? paramTypeAnn.value() : paramType, sessionAnn.create());
				if (session == null) throw new ParamValidationException("Session required");
				return session;
			}
			Src srcAnn = findAnnotation(paramAnnotations, Src.class);
			if (srcAnn != null) return c.src;
			return super.injectParam(paramType, paramAnnotations, context);
		}
	}

}
