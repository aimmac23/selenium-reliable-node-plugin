package com.mooo.aimmac23.hub.proxy;

import java.util.Map;

import org.openqa.grid.internal.RemoteProxy;

public interface ReliabilityAwareProxy extends RemoteProxy {

	public void setCapabilityAsWorking(Map<String, Object> capabilities);
	
	public void setCapabilityAsBroken(Map<String, Object> capabilities);
	
	public boolean isCapabilityBroken(Map<String, Object> capability);
	
	public boolean isCapabilityWorking(Map<String, Object> capabilities);
}
