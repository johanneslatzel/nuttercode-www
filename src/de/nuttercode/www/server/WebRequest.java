package de.nuttercode.www.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotEmpty;
import de.nuttercode.util.assurance.NotNull;

public class WebRequest extends WebObject {

	private RequestMethod method;
	private String uriBase;
	private final Map<String, String> uriParameterMap;

	public WebRequest() {
		this("/");
	}

	public WebRequest(String uriBase) {
		setUriBase(uriBase);
		setMethod(RequestMethod.GET);
		uriParameterMap = new HashMap<>();
	}

	public WebRequest(WebRequest request) {
		this.uriParameterMap = request.uriParameterMap;
		setUriBase(request.getUri());
		setMethod(request.getMethod());
		for (String field : request.getHeaderFieldNames())
			setHeaderField(field, request.getHeaderField(field));
		setBody(request.getBody());
	}

	public WebRequest(InputStream inputStream) throws ProtocolException, IOException {
		this();
		HttpStreamReader reader = new HttpStreamReader(inputStream);
		String line = reader.readLine();
		if (line == null || line.isEmpty())
			throw new ProtocolException("command line is missing: " + line);
		String[] commandSplit = line.split(" ");
		if (commandSplit.length != 3)
			throw new ProtocolException("command line has the wrong format: " + line);
		setMethod(RequestMethod.valueOf(commandSplit[0]));
		setUriBase(commandSplit[1]);
		if (!commandSplit[2].equals(WebServer.HTTP_VERSION))
			throw new ProtocolException("wrong http version, expected: " + WebServer.HTTP_VERSION);
		readHeader(reader);
		if (getMethod() != RequestMethod.HEAD)
			readBody(reader);
		interpretUri();
	}

	private void interpretUri() {
		int paramStart = uriBase.indexOf("?");
		if (paramStart >= 0) {
			for (String parameter : uriBase.substring(paramStart + 1).split("&")) {
				int parameterDelim = parameter.indexOf("=");
				if (parameterDelim > 0)
					uriParameterMap.put(parameter.substring(0, parameterDelim),
							parameter.substring(parameterDelim + 1));
			}
			uriBase = uriBase.substring(0, paramStart);
		}
	}

	public boolean hasUriParameter(String name) {
		return uriParameterMap.containsKey(name);
	}

	public String getUriParameter(String name) {
		return uriParameterMap.get(name);
	}

	public void addUriParameter(String name, String value) {
		if (value.contains("\n"))
			throw new IllegalArgumentException("parameter value contains linefeed");

		uriParameterMap.put(name, value);
	}

	public @NotEmpty String getUri() {
		StringBuilder uri = new StringBuilder();
		uri.append(uriBase);
		if (!uriParameterMap.isEmpty()) {
			boolean isFirst = true;
			uri.append('?');
			for (String name : uriParameterMap.keySet()) {
				if (isFirst)
					isFirst = false;
				else
					uri.append('&');
				uri.append(name);
				uri.append('=');
				uri.append(uriParameterMap.get(name));
			}
		}
		return uri.toString();
	}

	public @NotNull String getReducedUri() {
		return uriBase.substring(1);
	}

	public void setUriBase(@NotEmpty String uri) {
		Assurance.assureNotEmpty(uri);
		this.uriBase = uri;
	}

	public @NotNull RequestMethod getMethod() {
		return method;
	}

	public void setMethod(@NotNull RequestMethod method) {
		Assurance.assureNotNull(method);
		this.method = method;
	}

	public WebResponse sendTo(String hostname) throws ProtocolException, IOException {
		return sendTo(hostname, 80);
	}

	public WebResponse sendTo(String hostname, int port) throws ProtocolException, IOException {
		try (Socket socket = new Socket(hostname, port)) {
			return sendTo(socket);
		}
	}

	public WebResponse sendTo(Socket socket) throws ProtocolException, IOException {
		setHeaderField(WebServer.HF_HOST, socket.getInetAddress().getHostName());
		return sendTo(socket.getOutputStream(), socket.getInputStream());
	}

	public WebResponse sendTo(OutputStream outputStream, InputStream inputStream)
			throws ProtocolException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		writer.write(getMethod().toString());
		writer.write(' ');
		writer.write(getUri());
		writer.write(' ');
		writer.write(WebServer.HTTP_VERSION);
		writer.write(WebServer.CRLF);
		writeHeaderFields(writer);
		writer.flush();
		writeBody(outputStream);
		outputStream.flush();
		return new WebResponse(inputStream);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((uriBase == null) ? 0 : uriBase.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebRequest other = (WebRequest) obj;
		if (method != other.method)
			return false;
		if (uriBase == null) {
			if (other.uriBase != null)
				return false;
		} else if (!uriBase.equals(other.uriBase))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(method);
		builder.append(' ');
		builder.append(getUri());
		builder.append(' ');
		builder.append(WebServer.HTTP_VERSION);
		builder.append(WebServer.CRLF);
		for (String field : getHeaderFieldNames()) {
			builder.append(field);
			builder.append(": ");
			builder.append(getHeaderField(field));
			builder.append(WebServer.CRLF);
		}
		builder.append(WebServer.CRLF);
		return builder.toString();
	}

}
