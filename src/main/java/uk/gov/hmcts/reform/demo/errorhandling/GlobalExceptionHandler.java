package uk.gov.hmcts.reform.demo.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.PublicationNotFoundException;

import java.time.LocalDateTime;

/**
 * Global exception handler, that captures exceptions thrown by the controllers, and encapsulates
 * the logic to handle them and return a standardised response to the user
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Template exception handler, that handles a custom PublicationNotFoundException,
     * and returns a 404 in the standard format
     * @param ex The exception that has been thrown.
     * @param request The request made to the endpoint.
     * @return The error response, modelled using the ExceptionResponse object.
     */
    @ExceptionHandler(PublicationNotFoundException.class)
    ResponseEntity<ExceptionResponse> handlePublicationNotFound(PublicationNotFoundException ex, WebRequest request) {

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

}
