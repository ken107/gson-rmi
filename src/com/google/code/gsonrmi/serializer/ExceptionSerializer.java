package com.google.code.gsonrmi.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ExceptionSerializer implements JsonSerializer<Exception>, JsonDeserializer<Exception> {

	@Override
	public Exception deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		JsonObject o = json.getAsJsonObject();
		try {
			Class<?> clazz = Class.forName(o.get("class").getAsString());
			Constructor<?> cons = clazz.getConstructor(String.class);
			Exception src = (Exception) cons.newInstance(o.get("message").getAsString());
			StackTraceElement[] stackTrace = context.deserialize(o.get("stackTrace"), StackTraceElement[].class);
			src.setStackTrace(stackTrace);
			return src;
		}
		catch (Exception e) {
			//if we can't deserialize the error, then hell we'll just return the deserialization error
			return e;
		}
	}

	@Override
	public JsonElement serialize(Exception src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject o = new JsonObject();
		o.addProperty("class", src.getClass().getName());
		o.addProperty("message", src.getMessage());
		o.add("stackTrace", context.serialize(src.getStackTrace()));
		return o;
	}

}
