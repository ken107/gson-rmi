package com.google.code.gsonrmi;

import java.lang.reflect.Method;

public interface MethodLocator {

	Method get(Object target, String method, Parameter[] params);
}
