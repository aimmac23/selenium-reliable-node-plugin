package com.aimmac23.hub.proxy;

import java.util.Map;
import java.util.logging.Logger;

import org.openqa.grid.internal.utils.CapabilityMatcher;

public class ReliableCapabilityMatcher implements CapabilityMatcher {
	
	private static Logger log = Logger.getLogger(ReliableCapabilityMatcher.class.getName());
	
	private CapabilityMatcher innerMatcher;
	private ReliabilityAwareProxy proxy;

	public ReliableCapabilityMatcher(ReliabilityAwareProxy proxy, CapabilityMatcher innerMatcher) {
		this.proxy = proxy;
		this.innerMatcher = innerMatcher;
	}
	
	@Override
	public boolean matches(Map<String, Object> currentCapability,
			Map<String, Object> requestedCapability) {
		
		// to allow the node to be tested - we're calling enough internal
		// methods to ensure that we're calling this on the correct node
		if(requestedCapability.containsKey("proxyId")) {
			return innerMatcher.matches(currentCapability, requestedCapability);
		}
		
		// check to see if we're allowed to use that config
		if(!proxy.isCapabilityWorking(currentCapability)) {
			log.info("Node capability broken - not matching capability: " + currentCapability);
			return false;
		}
		
		// otherwise, match on the capabilities
		return innerMatcher.matches(currentCapability, requestedCapability);
	}
}
