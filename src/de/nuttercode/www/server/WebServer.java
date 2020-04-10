package de.nuttercode.www.server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import de.nuttercode.log.Log;
import de.nuttercode.log.LogException;
import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotNull;

public abstract class WebServer implements Closeable, WebRequestHandler {

	final static String CRLF = "\r\n";
	final static String HTTP_VERSION = "HTTP/1.1";
	final static String UTF_8 = "UTF-8";
	private final static int DEFAULT_SOCKET_TIMEOUT = 5_000;
	protected static final String HF_HOST = "Host";

	public static void run(WebServer server) throws FileNotFoundException, IOException {
		server.init();
		server.start();
	}

	private final File configurationFile;
	private ListenerThread listenerThread;
	private int socketTimeout;
	private String hostname;
	private Log log;
	private boolean devMode;
	private String logDirectory;

	public WebServer(@NotNull File configurationFile) {
		Assurance.assureNotNull(configurationFile);
		this.configurationFile = configurationFile;
		this.hostname = "unknown";
		listenerThread = null;
		socketTimeout = DEFAULT_SOCKET_TIMEOUT;
		log = null;
		setDevMode(false);
		logDirectory = null;
	}

	private void handleSocket(Socket socket) {
		try {
			socket.setSoTimeout(socketTimeout);
			WebResponse response = handleRequest(new WebRequest(socket.getInputStream()));
			if (response == null)
				response = WebResponse.from(ResponseCode.INTERNAL_SERVER_ERROR);
			response.setHeaderField(HF_HOST, getHostname());
			response.sendTo(socket.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readConfiguration() throws IOException {
		String line = null;
		int lineNumber = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(configurationFile))) {
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim().toLowerCase();
				switch (line) {
				case "[server]":
					readServerEntry(reader, lineNumber);
					break;
				default:
					break;
				}
			}
		}
	}

	private void readServerEntry(BufferedReader reader, int lineNumber) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim().toLowerCase();
			lineNumber++;
			if (line.equals("[/server]"))
				break;
			String[] split = line.split(":");
			if (split.length == 2) {
				split[0] = split[0].trim().toLowerCase();
				split[1] = split[1].trim().toLowerCase();
				switch (split[0]) {
				case "port":
					try {
						listenerThread.setPort(Integer.parseInt(split[1]));
					} catch (NumberFormatException e) {
						throw new IllegalStateException(
								"illegal port number in server segment: " + split[1] + " on line " + lineNumber, e);
					}
					break;
				case "log_directory":
					logDirectory = split[1];
					break;
				case "backlog":
					try {
						listenerThread.setPort(Integer.parseInt(split[1]));
					} catch (NumberFormatException e) {
						throw new IllegalStateException(
								"illegal port number in server segment: " + split[1] + " on line " + lineNumber, e);
					}
					break;
				case "hostname":
					setHostname(split[1]);
					break;
				default:
					break;
				}
			}
		}
	}

	private void setHostname(String hostname) {
		this.hostname = hostname;
	}

	protected void onInit() {
	}

	public void start() throws UnknownHostException, IOException {
		listenerThread.start();
	}

	/**
	 * 
	 * @throws IllegalArgumentException if the hostname in the config file has not
	 *                                  been configured properly
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void init() throws FileNotFoundException, IOException {
		close();
		listenerThread = new ListenerThread(this::handleSocket);
		readConfiguration();
		Assurance.assureNotEmpty(hostname);
		if (logDirectory != null)
			log = new Log(new File(logDirectory), hostname);
		onInit();
		if (log != null)
			log.logInfo("WebServer started");
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public boolean isDevMode() {
		return devMode;
	}

	public void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public boolean hasLog() {
		return log != null;
	}

	public Log getLog() {
		if (!hasLog())
			throw new LogException("log has not been configured");
		return log;
	}

	@Override
	public void close() throws IOException {
		if (listenerThread != null)
			listenerThread.close();
		if (log != null) {
			log.close();
		}
	}

	public String getHostname() {
		return hostname;
	}

}
