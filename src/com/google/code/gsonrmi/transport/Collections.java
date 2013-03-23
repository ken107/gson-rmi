package com.google.code.gsonrmi.transport;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Collections {

	public static interface Groupable<K> {
		K getGroupKey(Object groupBy);
	}
	
	public static <K, V extends Groupable<K>> Map<K, List<V>> group(Collection<V> elements, Object by) {
		Map<K, List<V>> out = new HashMap<K, List<V>>();
		for (V element : elements) {
			K key = ((Groupable<K>) element).getGroupKey(by);
			if (key != null) {
				List<V> group = out.get(key);
				if (group == null) out.put(key, group = new LinkedList<V>());
				group.add(element);
			}
		}
		return out;
	}
}
