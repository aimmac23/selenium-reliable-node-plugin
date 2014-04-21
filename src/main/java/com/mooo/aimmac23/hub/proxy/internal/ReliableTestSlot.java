package com.mooo.aimmac23.hub.proxy.internal;

import java.util.Map;

import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSlot;

public class ReliableTestSlot extends TestSlot {

	public ReliableTestSlot(RemoteProxy proxy, SeleniumProtocol protocol,
			String path, Map<String, Object> capabilities) {
		super(proxy, protocol, path, capabilities);
		// TODO Auto-generated constructor stub
	}

}
