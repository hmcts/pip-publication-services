package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;

import java.time.LocalDateTime;

/**
 * Global exception handler, that captures exceptions thrown by the controllers, and encapsulates
 * the logic to handle them and return a standardised response to the user.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Template exception handler, that handles a custom PublicationNotFoundException,
     * and returns a 404 in the standard format.
     * @return The error response, modelled using the ExceptionResponse object.
     */
    @ExceptionHandler(PublicationNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(
        PublicationNotFoundException ex) {

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(NotifyException.class)
    public ResponseEntity<ExceptionResponse> handle(NotifyException ex) {

        log.error(String.format("NotifyException was thrown with the init cause: %s", ex.getMessage()));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(BadPayloadException.class)
    public ResponseEntity<ExceptionResponse> handle(BadPayloadException ex) {

        log.error(String.format("BadPayloadException was thrown with the init cause: %s", ex.getCause()));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ExceptionResponse> handle(UnsupportedOperationException ex) {

        log.error(String.format("UnsupportedOperationException was thrown with the init cause: %s", ex.getCause()));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

}
