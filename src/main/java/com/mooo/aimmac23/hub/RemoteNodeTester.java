package com.mooo.aimmac23.hub;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.openqa.grid.web.Hub;

import com.mooo.aimmac23.hub.proxy.ReliableProxy;
import com.mooo.aimmac23.hub.servlet.NodeTestingServlet;

public class RemoteNodeTester {
	
	private static Logger log = Logger.getLogger(RemoteNodeTester.class.getName());

	private static final ThreadPoolExecutor executor;
	
	static {
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(1000));
	}
	public static void testRemoteNode(final ReliableProxy proxy, final Map<String, Object> capabilities) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				
				
				try {
					while(proxy.getRegistry().getProxyById(proxy.getId()) == null) {
						log.info("Proxy " + proxy.getId() + " not registred yet - waiting");
						Thread.sleep(500);
					}
					Hub hub = proxy.getRegistry().getHub();
					URL url = new URL(hub.getUrl(), "/grid/admin/" + NodeTestingServlet.class.getSimpleName() + "/session");
					BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", url.toExternalForm());
					
					HashMap<String, Object> transmittedCaps = new HashMap<String, Object>(capabilities);
					transmittedCaps.put("proxyId", proxy.getId());
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("desiredCapabilities", transmittedCaps);
					request.setEntity(new StringEntity(jsonObject.toString()));
					
					HttpClient client = proxy.getHttpClientFactory().getHttpClient();
					HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
					
					log.info("Hitting node testing servlet at: " + url);
					HttpResponse response = client.execute(httpHost, request);
					
					if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						log.info("Node tested OK: " + proxy.getId() + " for caps: " + capabilities);
						
					}
					else {
						log.warning("Node test failed for node: " + proxy.getId() + " for caps: " + capabilities);
						log.warning("Status code: " + response.getStatusLine().getStatusCode() + " body: " + EntityUtils.toString(response.getEntity()));
					}
						
				} catch(Exception e) {
					log.log(Level.WARNING, "Node test caught exception for node: " + proxy.getId() + " for caps: " + capabilities, e);
				}
			}
		});
		
	}
}
