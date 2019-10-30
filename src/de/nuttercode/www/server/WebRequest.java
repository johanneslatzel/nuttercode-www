package de.nuttercode.www.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotEmpty;
import de.nuttercode.util.assurance.NotNull;

public class WebRequest extends WebObject {

	private RequestMethod method;
	private String uri;
	private final Map<String, String> parameterMap;

	public WebRequest(WebRequest request) {
		this.parameterMap = request.parameterMap;
		setUri(request.getUri());
		setMethod(request.getMethod());
		for (String field : request.getHeaderFieldNames())
			setHeaderField(field, request.getHeaderField(field));
		setBody(request.getBody());
	}

	public WebRequest(InputStream inputStream) throws ProtocolException, IOException {
		parameterMap = new HashMap<>();
		HttpStreamReader reader = new HttpStreamReader(inputStream);
		String line = reader.readLine();
		if (line == null || line.isEmpty())
			throw new ProtocolException("command line is missing: " + line);
		String[] commandSplit = line.split(" ");
		if (commandSplit.length != 3)
			throw new ProtocolException("command line has the wrong format: " + line);
		setMethod(RequestMethod.valueOf(commandSplit[0]));
		setUri(commandSplit[1]);
		if (!commandSplit[2].equals(WebServer.HTTP_VERSION))
			throw new ProtocolException("wrong http version, expected: " + WebServer.HTTP_VERSION);
		readHeader(reader);
		if (getMethod() != RequestMethod.HEAD)
			readBody(reader);
		interpretUri();
	}

	private void interpretUri() {
		int paramStart = uri.indexOf("?");
		if (paramStart >= 0) {
			for (String parameter : uri.substring(paramStart + 1).split("&")) {
				int parameterDelim = parameter.indexOf("=");
				if (parameterDelim > 0)
					parameterMap.put(parameter.substring(0, parameterDelim), parameter.substring(parameterDelim + 1));
			}
		}
	}

	public boolean hasParameter(String name) {
		return parameterMap.containsKey(name);
	}

	public String getParameter(String name) {
		return parameterMap.get(name);
	}

	public @NotEmpty String getUri() {
		return uri;
	}

	public @NotNull String getReducedUri() {
		return uri.substring(1);
	}

	public void setUri(@NotEmpty String uri) {
		Assurance.assureNotEmpty(uri);
		this.uri = uri;
	}

	public @NotNull RequestMethod getMethod() {
		return method;
	}

	public void setMethod(@NotNull RequestMethod method) {
		Assurance.assureNotNull(method);
		this.method = method;
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
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(method);
		builder.append(' ');
		builder.append(uri);
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
