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
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import com.mooo.aimmac23.hub.proxy.internal.ReliableTestSlot;

public class ReliableProxy extends DefaultRemoteProxy {
	
	private Set<Map<String, Object>> testedCapabilities = new HashSet<Map<String, Object>>();
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
		
		if(session.getExternalKey() == null) {
			// session creation failed - we should do something about this
			System.out.println("Session creation failed");
		}
	}

}
