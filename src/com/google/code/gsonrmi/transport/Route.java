package com.google.code.gsonrmi.transport;

import java.net.URI;
import java.util.LinkedList;

import com.google.code.gsonrmi.Parameter;
import com.google.code.gsonrmi.transport.Collections.Groupable;

public class Route implements Groupable<String> {
	
	public static enum GroupBy {
		SCHEME,
		AUTHORITY
	}

	public final LinkedList<URI> hops;
	public Parameter trackingId;
	
	public Route(URI... hops) {
		this.hops = new LinkedList<URI>();
		for (URI hop : hops) this.hops.add(hop);
	}
	
	public Route(Route clone) {
		hops = new LinkedList<URI>(clone.hops);
		trackingId = clone.trackingId;
	}
	
	public Route setTrackingId(Object o) {
		trackingId = new Parameter(o);
		return this;
	}

	@Override
	public String getGroupKey(Object groupBy) {
		if (GroupBy.SCHEME.equals(groupBy)) return hops.isEmpty() ? null : hops.getFirst().getScheme();
		else if (GroupBy.AUTHORITY.equals(groupBy)) return hops.isEmpty() ? null : hops.getFirst().getAuthority();
		else return null;
	}
}
