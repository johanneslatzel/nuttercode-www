package de.nuttercode.www.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotNull;

class ListenerThread implements Closeable {

	private final static int DEFAULT_BACKLOG = 10;
	private final static int DEFAULT_PORT = 80;

	private ServerSocket serverSocket;
	private int port;
	private int backlog;
	private final Consumer<Socket> socketHandler;
	private final Thread thread;

	ListenerThread(@NotNull Consumer<Socket> socketHandler) {
		Assurance.assureNotNull(socketHandler);
		this.socketHandler = socketHandler;
		thread = new Thread(this::run);
		port = DEFAULT_PORT;
		backlog = DEFAULT_BACKLOG;
	}

	private void run() {
		try {
			serverSocket = new ServerSocket(port, backlog);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		while (!serverSocket.isClosed()) {
			try {
				final Socket socket = serverSocket.accept();
				new Thread(() -> {
					socketHandler.accept(socket);
					try {
						socket.close();
					} catch (IOException e) {
					}
				}).start();
			} catch (IOException e) {
			}
		}
	}

	void setPort(int port) {
		this.port = port;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	@Override
	public void close() throws IOException {
		if (serverSocket != null)
			serverSocket.close();
		if (thread.isAlive())
			thread.interrupt();
	}

	public void start() {
		thread.start();
	}

	public int getPort() {
		return port;
	}

}
