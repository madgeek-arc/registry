package gr.uoa.di.madgik.registry.exception;

import org.springframework.http.HttpStatus;

/**
 * General resource exception.
 */
public class ResourceException extends RuntimeException {
    private HttpStatus status;

    /**
     * Constructs a ResourceException having the specified {@link HttpStatus status} code.
     */
    public ResourceException(HttpStatus status) {
        this("ResourceException", status);
    }

    /**
     * Constructs a ResourceException having a custom message and the specified {@link HttpStatus status} code.
     */
    public ResourceException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    /**
     * Constructs a ResourceException from an existing {@link Exception} having the specified {@link HttpStatus status} code.
     */
    public ResourceException(Exception e, HttpStatus status) {
        super(e);
        this.setStatus(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    private void setStatus(HttpStatus status) {
        this.status = status;
    }
}
