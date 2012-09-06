package ca.uhn.ws.security;

import ca.uhn.ws.model.ServiceInfo;

import com.google.gson.JsonObject;

public interface ISecurityManager {

	public void authenticate(ServiceInfo serviceInfo, String method, JsonObject params) throws Exception;
}
