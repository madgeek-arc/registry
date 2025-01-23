package gr.uoa.di.madgik.registry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to create a resource using an existing id.
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a ResourceAlreadyExistsException with default message.
     */
    public ResourceAlreadyExistsException() {
        super("Resource Already Exists");
    }

    /**
     * Constructs a ResourceAlreadyExistsException displaying the conflicting id.
     */
    public ResourceAlreadyExistsException(String id) {
        super(String.format("Resource with [id=%s] already exists", id));
    }

    /**
     * Constructs a ResourceAlreadyExistsException displaying the resourceType and the conflicting id.
     */
    public ResourceAlreadyExistsException(String id, String resourceType) {
        super(String.format("[resourceType=%s] with [id=%s] already exists", resourceType, id));
    }
}
