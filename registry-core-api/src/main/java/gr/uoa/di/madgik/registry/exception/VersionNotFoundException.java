package gr.uoa.di.madgik.registry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by stefanos on 15-Nov-16.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException() {
        super("Versions Not Found");
    }
}