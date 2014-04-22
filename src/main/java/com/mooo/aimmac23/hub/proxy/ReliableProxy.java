package com.mooo.aimmac23.hub.proxy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.BaseRemoteProxy;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.listeners.SelfHealingProxy;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import com.mooo.aimmac23.hub.RemoteNodeTester;
import com.mooo.aimmac23.hub.proxy.internal.ReliableTestSlot;

public class ReliableProxy extends DefaultRemoteProxy implements SelfHealingProxy {
	
	private Set<Map<String, Object>> brokenCapabilities = new HashSet<Map<String, Object>>();
	private Set<Map<String, Object>> workingCapabilities = new HashSet<Map<String, Object>>();
	
	
	public ReliableProxy(RegistrationRequest request, Registry registry) {
		super(request, registry);
		
		
		try {
			List<TestSlot> oldSlots = getTestSlots();
			List<ReliableTestSlot> newSlots = new ArrayList<ReliableTestSlot>();
			
			for(TestSlot slot : oldSlots) {
				newSlots.add(new ReliableTestSlot(this, slot.getProtocol(), slot.getPath(), slot.getCapabilities()));
			}
			
			
			Field slotFields = BaseRemoteProxy.class.getDeclaredField("testSlots");
			slotFields.setAccessible(true);
			slotFields.set(this, newSlots);
		} catch(Exception e) {
			throw new IllegalStateException(e);
		}		
	}
	
	@Override
	public void afterSession(TestSession session) {
		super.afterSession(session);
		
		Map<String, Object> capabilities = session.getSlot().getCapabilities();

		if(session.getExternalKey() == null && workingCapabilities.contains(capabilities)) {
			// session creation failed on a slot we thought was working - we should do something about this
			System.out.println("Session creation failed, re-testing configuration: " + capabilities);
			setCapabilityAsBroken(capabilities);
			RemoteNodeTester.testRemoteNode(this, capabilities);
		}
	}
	
	@Override
	public void startPolling() {
		super.startPolling();
		
		// Bit of an abuse of this method, but we shouldn't start verifying things until 
		// the node has been properly registered
		HashSet<Map<String, Object>> allCapabilities = new HashSet<Map<String, Object>>();
		for(TestSlot slot : getTestSlots()) {
			allCapabilities.add(slot.getCapabilities());
		}
		
		for(Map<String, Object> caps : allCapabilities) {
			RemoteNodeTester.testRemoteNode(this, caps);

		}
	}
	
	public void setCapabilityAsWorking(Map<String, Object> capabilities) {
		brokenCapabilities.remove(capabilities);
		workingCapabilities.add(capabilities);
	}
	
	public void setCapabilityAsBroken(Map<String, Object> capabilities) {
		brokenCapabilities.add(capabilities);
		workingCapabilities.remove(capabilities);
	}
	
	public boolean isCapabilityUsable(Map<String, Object> capabilities) {
		return workingCapabilities.contains(capabilities) && !brokenCapabilities.contains(capabilities);
	}
	
	@Override
	public CapabilityMatcher getCapabilityHelper() {
		return new ReliableCapabilityMatcher(this, super.getCapabilityHelper());
	}
	
	public static class ReliableCapabilityMatcher implements CapabilityMatcher {
		
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

}
