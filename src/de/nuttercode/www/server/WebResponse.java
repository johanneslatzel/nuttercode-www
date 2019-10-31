package de.nuttercode.www.server;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotNull;

public class WebResponse extends WebObject {

	public static WebResponse from(ResponseCode code) {
		WebResponse response = new WebResponse(code.getCode(), code.getMessage());
		try {
			response.setBody("<html><h1>" + code.getMessage() + "</h1></html>");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		return response;
	}

	private static final String DEFAULT_STATUS_REASON = "OK";
	private static final int DEFAULT_STATUS_CODE = 200;
	private static final String HF_EXPIRES = "Expires";
	private static final String HF_CONTENT_ENCODING = "Content-Encoding";
	private final static String HF_ACCEPT_ENCODING = "Accept-Encoding";
	private final static String GZIP = "gzip";

	private int statusCode;
	private String statusReason;

	public WebResponse() {
		this(DEFAULT_STATUS_CODE, DEFAULT_STATUS_REASON);
	}

	public WebResponse(String content) throws UnsupportedEncodingException {
		this();
		setBody(content);
	}

	public WebResponse(int statusCode, @NotNull String statusReason) {
		setStatusCode(statusCode);
		setStatusReason(statusReason);
	}

	public WebResponse(InputStream inputStream) throws ProtocolException, IOException {
		HttpStreamReader reader = new HttpStreamReader(inputStream);
		String line = reader.readLine();
		if (line == null || line.isEmpty())
			throw new ProtocolException("status line is missing");
		int spacePosition = line.indexOf(' ');
		if (spacePosition == -1 || spacePosition == line.length() - 1)
			throw new ProtocolException("invalid status line: " + line);
		if (!line.substring(0, spacePosition).equals(WebServer.HTTP_VERSION))
			throw new ProtocolException("wrong http version, expected: " + WebServer.HTTP_VERSION);
		if (spacePosition == line.length() - 1)
			throw new ProtocolException("missing status code in line: " + line);
		String subline = line.substring(spacePosition + 1);
		spacePosition = subline.indexOf(' ');
		if (spacePosition == -1)
			throw new ProtocolException("invalid status line: " + line);
		String statusCode = subline.substring(0, spacePosition);
		try {
			setStatusCode(Integer.parseInt(statusCode));
		} catch (NumberFormatException e) {
			throw new ProtocolException("not a status code: " + statusCode);
		}
		if (spacePosition == subline.length() - 1)
			throw new ProtocolException("no status reason supplied: " + line);
		setStatusReason(subline.substring(spacePosition + 1));
		readHeader(reader);
		readBody(reader);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public @NotNull String getStatusReason() {
		return statusReason;
	}

	public void setStatusCode(int errorCode) {
		this.statusCode = errorCode;
	}

	public void setStatusReason(@NotNull String statusReason) {
		Assurance.assureNotNull(statusReason);
		this.statusReason = statusReason;
	}

	public String getStatusLine() {
		StringBuilder builder = new StringBuilder(100);
		builder.append("HTTP/1.1 ");
		builder.append(statusCode);
		builder.append(' ');
		builder.append(statusReason);
		return builder.toString();
	}

	public void expire(WebRequest request, int expirationHours) {
		if (expirationHours < 0)
			return;
		setHeaderField(HF_EXPIRES, DateTimeFormatter.RFC_1123_DATE_TIME
				.format(ZonedDateTime.now(ZoneOffset.UTC).plusHours(expirationHours)));
	}

	public void compressWithGZip(WebRequest request) {
		String acceptEncoding = request.getHeaderField(HF_ACCEPT_ENCODING);
		if (acceptEncoding == null || !acceptEncoding.contains(GZIP))
			return;
		setHeaderField(HF_CONTENT_ENCODING, GZIP);
		byte[] body = getBody();
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(body.length)) {
			try (GZIPOutputStream gout = new GZIPOutputStream(outputStream)) {
				gout.write(body);
				gout.flush();
			}
			outputStream.flush();
			setBody(outputStream.toByteArray());
		} catch (IOException e) {
			return;
		}
	}

	public void sendTo(OutputStream outputStream) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		writer.write(WebServer.HTTP_VERSION);
		writer.write(' ');
		writer.write(Integer.toString(getStatusCode()));
		writer.write(' ');
		writer.write(getStatusReason());
		writer.write(WebServer.CRLF);
		writeHeaderFields(writer);
		writer.flush();
		writeBody(outputStream);
		outputStream.flush();
	}

	public boolean isOk() {
		return statusCode == ResponseCode.OK.getCode();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + statusCode;
		result = prime * result + ((statusReason == null) ? 0 : statusReason.hashCode());
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
		WebResponse other = (WebResponse) obj;
		if (statusCode != other.statusCode)
			return false;
		if (statusReason == null) {
			if (other.statusReason != null)
				return false;
		} else if (!statusReason.equals(other.statusReason))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(WebServer.HTTP_VERSION);
		builder.append(' ');
		builder.append(Integer.toString(statusCode));
		builder.append(' ');
		builder.append(statusReason);
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
