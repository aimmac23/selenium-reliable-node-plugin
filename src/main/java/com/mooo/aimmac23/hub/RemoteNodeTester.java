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

import com.mooo.aimmac23.hub.proxy.ReliabilityAwareProxy;
import com.mooo.aimmac23.hub.proxy.ReliableProxy;
import com.mooo.aimmac23.hub.servlet.NodeTestingServlet;

public class RemoteNodeTester {
	
	private static Logger log = Logger.getLogger(RemoteNodeTester.class.getName());

	private static final ThreadPoolExecutor executor;
	
	static {
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(1000));
	}
	public static void testRemoteNode(final ReliabilityAwareProxy proxy, final Map<String, Object> capabilities) {
		executor.execute(new Runnable() {
			
			private int runAttempts = 0;
			
			private void resubmitJob() throws InterruptedException {
				// resubmit job
				executor.submit(this);
				// prevent fast spinning
				Thread.sleep(500);
				runAttempts++;
				return;
			}
			@Override
			public void run() {
				
				if(runAttempts > 10) {
					log.warning("Maximum slot test attempts exceeded - marking as failed. proxy: " + 
							proxy.getId() + " capabilities: " + capabilities);
					proxy.setCapabilityAsBroken(capabilities);
					return;
				}
				
				try {
					while(proxy.getRegistry().getProxyById(proxy.getId()) == null) {
						log.info("Proxy " + proxy.getId() + " not registered yet - deferring test");
						resubmitJob();
						return;
					}
					
					if(100.0 == proxy.getResourceUsageInPercent()) {
						log.info("Proxy " + proxy.getId() + " is busy - deferring test");
						resubmitJob();
						return;
						
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
					
					int statusCode = response.getStatusLine().getStatusCode();
					
					if(statusCode == HttpStatus.SC_OK) {
						log.info("Node tested OK: " + proxy.getId() + " for caps: " + capabilities);
						proxy.setCapabilityAsWorking(capabilities);
						
					}
					else if(statusCode == 429) {
						// test slot is temporarily unavailable - race condition?
						log.warning("Test slot temporarily unavailable on proxy: " + proxy.getId() + " capabilities: " + capabilities);
						resubmitJob();
					}
					else {
						log.warning("Node test failed for node: " + proxy.getId() + " for caps: " + capabilities);
						log.warning("Status code: " + response.getStatusLine().getStatusCode() + " body: " + EntityUtils.toString(response.getEntity()));
						proxy.setCapabilityAsBroken(capabilities);
					}
						
				} catch(Exception e) {
					log.log(Level.WARNING, "Node test caught exception for node: " + proxy.getId() + " for caps: " + capabilities, e);
				}
			}
		});
		
	}
}
