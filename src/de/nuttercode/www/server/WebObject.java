package de.nuttercode.www.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotEmpty;
import de.nuttercode.util.assurance.NotNull;

abstract class WebObject {

	private final static byte[] DEFAULT_BODY = new byte[0];
	private static final char COLON = ':';
	private final static String HF_CONTENT_LENGTH = "Content-Length";

	private byte[] body;
	private final Map<String, String> headerFields;

	WebObject() {
		body = DEFAULT_BODY;
		headerFields = new HashMap<>();
	}

	protected void readBody(HttpStreamReader reader) throws IOException {
		String cl = getHeaderField(HF_CONTENT_LENGTH);
		if (cl == null || cl.isEmpty())
			return;
		int length = Integer.parseInt(cl);
		if (length < 0)
			throw new ProtocolException("negative value for field: " + HF_CONTENT_LENGTH);
		if (length == 0)
			return;
		setBody(reader.readBytes(length));
	}

	protected void readHeader(HttpStreamReader reader) throws ProtocolException, IOException {
		String line;
		while ((line = reader.readLine()) != null && !line.isEmpty()) {
			int colonPosition = line.indexOf(COLON);
			if (colonPosition == -1)
				throw new ProtocolException("wrong header field format, expected colon in: " + line);
			if (colonPosition == line.length())
				throw new ProtocolException("empty header field: " + line);
			setHeaderField(line.substring(0, colonPosition), line.substring(colonPosition + 1, line.length()));
		}
	}

	public @NotNull byte[] getBody() {
		return body;
	}

	public void setBody(@NotNull String body, @NotEmpty String charsetName) throws UnsupportedEncodingException {
		Assurance.assureNotNull(body);
		Assurance.assureNotEmpty(charsetName);
		setBody(body.getBytes(charsetName));
	}

	public void setBody(@NotNull String body) throws UnsupportedEncodingException {
		setBody(body, WebServer.UTF_8);
	}

	public void setBody(@NotNull byte[] body) {
		Assurance.assureNotNull(body);
		this.body = body;
		setHeaderField(HF_CONTENT_LENGTH, Integer.toString(body.length));
	}

	public void setHeaderField(@NotEmpty String field, @NotEmpty String value) {
		Assurance.assureNotNull(field);
		field = field.trim();
		Assurance.assureNotEmpty(field);
		Assurance.assureNotNull(value);
		value = value.trim();
		Assurance.assureNotEmpty(value);
		headerFields.put(field, value);
	}

	public String getHeaderField(String field) {
		return headerFields.get(field);
	}

	public Set<String> getHeaderFieldNames() {
		return Collections.unmodifiableSet(headerFields.keySet());
	}

	public void writeHeaderFields(BufferedWriter writer) throws IOException {
		for (String field : getHeaderFieldNames()) {
			writer.write(field);
			writer.write(": ");
			writer.write(getHeaderField(field));
			writer.write(WebServer.CRLF);
		}
		writer.write(WebServer.CRLF);
	}

	public void writeBody(OutputStream outputStream) throws IOException {
		if (body != null)
			outputStream.write(body);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(body);
		result = prime * result + ((headerFields == null) ? 0 : headerFields.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebObject other = (WebObject) obj;
		if (!Arrays.equals(body, other.body))
			return false;
		if (headerFields == null) {
			if (other.headerFields != null)
				return false;
		} else if (!headerFields.equals(other.headerFields))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WebObject [body=" + Arrays.toString(body) + ", headerFields=" + headerFields + "]";
	}

}
