package com.google.code.gsonrmi.transport;

import java.net.URI;
import java.util.Arrays;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Collections.Groupable;

public class Route implements Groupable<String> {
	
	public static enum GroupBy {
		SCHEME,
		AUTHORITY
	}

	public final URI[] hops;
	public final Parameter trackingId;
	
	public Route(URI... hops) {
		this(null, hops);
	}
	
	public Route(Object trackingId, URI... hops) {
		this.trackingId = new Parameter(trackingId);
		this.hops = hops;
	}
	
	public Route addFirst(URI... hops) {
		URI[] out = Arrays.copyOf(hops, hops.length+this.hops.length);
		for (int i=0; i<this.hops.length; i++) out[hops.length+i] = this.hops[i];
		return new Route(trackingId, out);
	}
	
	public Route removeFirst() {
		return new Route(trackingId, Arrays.copyOfRange(hops, 1, hops.length));
	}
	
	public boolean isEmpty() {
		return hops.length == 0;
	}

	@Override
	public String getGroupKey(Object groupBy) {
		if (GroupBy.SCHEME.equals(groupBy)) return hops.length == 0 ? null : hops[0].getScheme();
		else if (GroupBy.AUTHORITY.equals(groupBy)) return hops.length == 0 ? null : hops[0].getAuthority();
		else return null;
	}
}
