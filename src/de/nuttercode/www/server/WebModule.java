package de.nuttercode.www.server;

import de.nuttercode.util.assurance.Assurance;
import de.nuttercode.util.assurance.NotEmpty;

class WebModule {

	private final String identification;
	private final int port;
	private final String hostname;

	public WebModule(String identification, @NotEmpty String hostname, String port) {
		Assurance.assureNotEmpty(hostname);
		this.identification = identification;
		try {
			this.port = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("port has the wrong format: " + port, e);
		}
		this.hostname = hostname;
	}

	public String getIdentification() {
		return identification;
	}

	public int getPort() {
		return port;
	}

	public String getHostname() {
		return hostname;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
		result = prime * result + ((identification == null) ? 0 : identification.hashCode());
		result = prime * result + port;
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
		WebModule other = (WebModule) obj;
		if (hostname == null) {
			if (other.hostname != null)
				return false;
		} else if (!hostname.equals(other.hostname))
			return false;
		if (identification == null) {
			if (other.identification != null)
				return false;
		} else if (!identification.equals(other.identification))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WebModule [identification=" + identification + ", port=" + port + ", hostname=" + hostname + "]";
	}

}
