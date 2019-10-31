package de.nuttercode.www.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import de.nuttercode.util.StackTraceConverter;
import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotEmpty;
import de.nuttercode.util.assurance.NotNull;

public abstract class WebServer implements Closeable {

	final static String CRLF = "\r\n";
	final static String HTTP_VERSION = "HTTP/1.1";
	final static String UTF_8 = "UTF-8";
	private final static int DEFAULT_SOCKET_TIMEOUT = 5_000;
	private static final String HF_HOST = "Host";

	public static void run(WebServer server) throws FileNotFoundException, IOException {
		server.init();
		server.start();
	}

	private final File configurationFile;
	private final Map<String, WebModule> modules;
	private ListenerThread listenerThread;
	private int socketTimeout;
	private boolean verbose;
	private final String hostname;
	private BufferedWriter log;

	public WebServer(String configurationFilePath, String hostname) {
		this(new File(configurationFilePath), hostname);
	}

	public WebServer(@NotNull File configurationFile, @NotEmpty String hostname) {
		Assurance.assureNotNull(configurationFile);
		Assurance.assureNotEmpty(hostname);
		this.configurationFile = configurationFile;
		this.hostname = hostname;
		modules = new HashMap<>();
		listenerThread = null;
		socketTimeout = DEFAULT_SOCKET_TIMEOUT;
		setVerbose(false);
		log = null;
	}

	private void handleSocket(Socket socket) {
		try {
			if (verbose)
				log("new socket: " + socket);
			socket.setSoTimeout(socketTimeout);
			WebRequest request = new WebRequest(socket.getInputStream());
			String reducedUri = request.getUri().substring(1);
			WebResponse response;
			int slashPosition = reducedUri.indexOf('/');
			if (slashPosition != -1) {
				WebModule module = findModule(reducedUri);
				if (module == null) {
					response = WebResponse.from(ResponseCode.NOT_FOUND);
				} else {
					try (Socket forwardSocket = new Socket(InetAddress.getByName(module.getHostname()),
							module.getPort())) {
						forwardSocket.setSoTimeout(socketTimeout);
						WebRequest forwardRequest = new WebRequest(request);
						forwardRequest.setUri(reducedUri.substring(slashPosition));
						response = forwardRequest.sendTo(forwardSocket.getOutputStream(),
								forwardSocket.getInputStream());
					} catch (IOException e) {
						response = WebResponse.from(ResponseCode.INTERNAL_SERVER_ERROR);
						if (verbose)
							log(e);
					}
				}
			} else {
				response = handleRequest(request);
			}
			if (response == null)
				response = WebResponse.from(ResponseCode.INTERNAL_SERVER_ERROR);
			response.setHeaderField(HF_HOST, getHostname());
			response.sendTo(socket.getOutputStream());
		} catch (Exception e) {
			if (verbose)
				e.printStackTrace();
		}
	}

	private WebModule findModule(String uri) {
		String value = uri.split("/")[0];
		synchronized (modules) {
			return modules.get(value);
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
				case "[module]":
					readModuleEntry(reader, lineNumber);
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
				case "log":
					try {
						log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(split[1])));
					} catch (NumberFormatException e) {
						throw new IllegalStateException(
								"illegal port number in server segment: " + split[1] + " on line " + lineNumber, e);
					}
					break;
				case "backlog":
					try {
						listenerThread.setPort(Integer.parseInt(split[1]));
					} catch (NumberFormatException e) {
						throw new IllegalStateException(
								"illegal port number in server segment: " + split[1] + " on line " + lineNumber, e);
					}
					break;
				default:
					break;
				}
			}
		}
	}

	private void readModuleEntry(BufferedReader reader, int lineNumber) throws IOException {
		String hostname = null;
		String port = null;
		String identification = null;
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim().toLowerCase();
			lineNumber++;
			if (line.equals("[/module]"))
				break;
			int colonPos = line.indexOf(':');
			if (colonPos < 0)
				continue;
			if (colonPos == 0)
				throw new IllegalStateException("no attribute name sipplied at line " + lineNumber);
			if (colonPos == line.length())
				throw new IllegalStateException("no attribute value supplied at line " + lineNumber);
			String name = line.substring(0, colonPos).trim().toLowerCase();
			String value = line.substring(colonPos + 1).trim().toLowerCase();
			switch (name) {
			case "port":
				port = value;
				break;
			case "identification":
				identification = value;
				break;
			case "hostname":
				hostname = value;
				break;
			default:
				break;
			}
		}
		modules.put(identification, new WebModule(identification, hostname, port));
	}

	protected abstract WebResponse handleRequest(WebRequest request);

	public void start() throws UnknownHostException, IOException {
		listenerThread.start();
		log("listener thread started");
	}

	public void init() throws FileNotFoundException, IOException {
		close();
		listenerThread = new ListenerThread(this::handleSocket);
		readConfiguration();
		log("initializaton done");
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void log(Exception e) {
		log(e.getMessage());
		log(StackTraceConverter.getStackTrace(e.getStackTrace()));
	}

	public void log(String message) {
		if (log == null)
			return;
		try {
			log.write(Instant.now().toString());
			log.write(": ");
			log.write(message);
			log.write('\n');
			log.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		if (listenerThread != null)
			listenerThread.close();
		if (log != null) {
			log.flush();
			log.close();
		}
		modules.clear();
	}

	public String getHostname() {
		return hostname;
	}

}
