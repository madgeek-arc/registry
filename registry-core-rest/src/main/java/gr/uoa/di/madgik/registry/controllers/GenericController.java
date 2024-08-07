package gr.uoa.di.madgik.registry.controllers;

/**
 * Created by stefanos on 2/2/2017.
 */


import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.exception.ServerError;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * @author stefanos
 * It is a base controller for Error handling messages;
 */
@Deprecated
//@ControllerAdvice
public class GenericController {


    private static final Logger logger = LoggerFactory.getLogger(GenericController.class);


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    ServerError handleBadRequest(HttpServletRequest req, Exception ex) {
        logger.info("Not Found", ex);
        return new ServerError(req.getRequestURL().toString(), ex);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ServerError handleServiceException(HttpServletRequest req, Exception ex) {
        logger.info("service exception", ex);
        return new ServerError(req.getRequestURL().toString(), ex);
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ServerError defaultException(HttpServletRequest req, Exception ex) {
        logger.error("Default exception handler", ex);
        return new ServerError(req.getRequestURL().toString(), ex);
    }
}
