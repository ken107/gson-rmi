package com.google.code.gsonrmi;

import java.net.URI;

public interface ObjectLocator {

	Object get(URI requestUri);
}
