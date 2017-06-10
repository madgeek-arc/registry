package eu.openminted.registry.core.service;

/**
 * Created by antleb on 5/26/16.
 */
public class ServiceException extends RuntimeException {

	private String error;

	private String errorDescription;

	public ServiceException() {
	}

	public ServiceException(String error, String errorDescription) {
		super(error + " : " + errorDescription);
		this.error = error;
		this.errorDescription = errorDescription;

	}

	public ServiceException(String message) {
		super(message);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}

	public String getError() {
		return error;
	}

	public String getErrorDescription() {
		return errorDescription;
	}
}
