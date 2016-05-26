package eu.openminted.registry.core.service;

/**
 * Created by antleb on 5/26/16.
 */
public class ServiceException extends Exception {

	public ServiceException() {
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
}
