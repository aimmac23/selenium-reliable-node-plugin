package com.mooo.aimmac23.hub.proxy;

import java.util.Map;

import org.openqa.grid.internal.utils.CapabilityMatcher;

public class ReliableCapabilityMatcher implements CapabilityMatcher {
	
	private CapabilityMatcher innerMatcher;
	private ReliableProxy proxy;

	public ReliableCapabilityMatcher(ReliableProxy proxy, CapabilityMatcher innerMatcher) {
		this.proxy = proxy;
		this.innerMatcher = innerMatcher;
	}
	
	@Override
	public boolean matches(Map<String, Object> currentCapability,
			Map<String, Object> requestedCapability) {
		
		// to allow the node to be tested
		if(requestedCapability.containsKey("proxyId")) {
			return innerMatcher.matches(currentCapability, requestedCapability);
		}
		
		// check to see if we're allowed to use that config
		if(!proxy.isCapabilityUsable(currentCapability)) {
			return false;
		}
		
		// otherwise, match on the capabilities
		return innerMatcher.matches(currentCapability, requestedCapability);
	}
}
