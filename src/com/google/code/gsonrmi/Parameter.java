package com.google.code.gsonrmi;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

public class Parameter {

	private Object value;
	private Type type;
	private JsonElement serializedValue;
	
	public Parameter(Object value) {
		this.value = value;
		if (value != null) type = (value instanceof Exception) ? Exception.class : value.getClass();
		else type = Object.class;
	}
	
	public Parameter(Object value, Type type) {
		this.value = value;
		this.type = (value != null && value instanceof Exception) ? Exception.class : type;
	}
	
	public Parameter(JsonElement serializedValue) {
		if (serializedValue == null) throw new NullPointerException("You probably want to invoke the other constructor!");
		this.serializedValue = serializedValue;
	}
	
	public Object getValue(Type type, Gson deserializer) {
		if (value == null && serializedValue != null) {
			value = deserializer.fromJson(serializedValue, type);
			this.type = type;
			serializedValue = null;
		}
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> type, Gson deserializer) {
		if (value == null && serializedValue != null) {
			value = deserializer.fromJson(serializedValue, type);
			this.type = type;
			serializedValue = null;
		}
		return (T) value;
	}
	
	public JsonElement getSerializedValue(Gson serializer) {
		if (serializedValue == null) serializedValue = serializer.toJsonTree(value, type);
		return serializedValue;
	}
	
	public JsonElement getSerializedValue(JsonSerializationContext context) {
		if (serializedValue == null) serializedValue = context.serialize(value, type);
		return serializedValue;
	}
}
