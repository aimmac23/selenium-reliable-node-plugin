package com.mooo.aimmac23.hub.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.grid.web.servlet.handler.WebDriverRequest;

public class NodeTestingServlet extends RegistryBasedServlet {
	
	private static Logger log = Logger.getLogger(NodeTestingServlet.class.getName());

	public NodeTestingServlet(Registry registry) {
		super(registry);
	}
	
	// XXX: Not sure this is a good idea, but I caught RegistrationServlet doing it
	public NodeTestingServlet() {
		this(null);
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
	      SeleniumBasedRequest request = new OurRegistrationRequest(req, getRegistry());
	      
	      Map<String, Object> capabilities = request.extractDesiredCapability();
	      
	      RemoteProxy proxyById = getRegistry().getProxyById((String)capabilities.get("proxyId"));
	      
	      if(proxyById == null) {
	    	  resp.sendError(HttpStatus.SC_BAD_REQUEST, "Remote proxy not found: " + capabilities.get("proxyId"));
	    	  return;
	      }
	      
	      TestSession session = proxyById.getNewSession(capabilities);
	      
	      session.forward(request, resp, true);
	      
	      log.info("External key: " + session.getExternalKey());

	}
	
	private static class OurRegistrationRequest extends WebDriverRequest {

		public OurRegistrationRequest(HttpServletRequest httpServletRequest,
				Registry registry) {
			super(httpServletRequest, registry);
		}
		
		@Override
		public RequestType extractRequestType() {
			return RequestType.START_SESSION;
		}
		
	}
}
