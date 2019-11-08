package de.nuttercode.www.server;

public enum ResponseCode {

	ACCESS_DENIED("access denied", 403), NOT_FOUND("not found", 404),
	INTERNAL_SERVER_ERROR("internal server error", 500), BAD_REQUEST("bad request", 400), OK("ok", 200),
	UNAUTHORIZED("unauthorized", 401);

	private final String message;
	private final int code;

	ResponseCode(String message, int code) {
		this.message = message;
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public int getCode() {
		return code;
	}

}
