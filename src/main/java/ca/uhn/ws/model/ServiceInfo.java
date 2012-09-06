package ca.uhn.ws.model;

import java.net.InetAddress;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public class ServiceInfo {
	
	private HttpServletRequest request;
	private ServletContext context;
	private String operationName;
	protected static String serverHostname = null;
	
	public ServiceInfo(HttpServletRequest request, ServletContext context, String operationName) {
		this.request = request;
		this.context = context;
		this.operationName = operationName;		
	}

	public HttpServletRequest getRequest() {
		return request;
	}
	
	public ServletContext getContext() {
		return context;
	}

	public String getOperationName() {
		return operationName;
	}
	
	public String getAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "unknown server IP";
		} 	
	}
	
	public String getHostname() {
		if(serverHostname != null)
			return serverHostname;		
		try {
			serverHostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception e1) {
			return getAddress();	// fall-back to IP address in case host is not in DNS
		}
		return serverHostname;
	}
}
