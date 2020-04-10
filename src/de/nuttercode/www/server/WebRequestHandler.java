package de.nuttercode.www.server;

public interface WebRequestHandler {
	
	WebResponse handleRequest(WebRequest request);

}
