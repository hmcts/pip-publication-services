package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

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
    public ResponseEntity<ExceptionResponse> handle(PublicationNotFoundException ex) {

        log.error(writeLog("404, publication has not been found when trying to send subscription"));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(NotifyException.class)
    public ResponseEntity<ExceptionResponse> handle(NotifyException ex) {

        log.error(writeLog(String.format(
            "400, Error while communicating with the notify service with reason %s", ex.getMessage())));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handle(MethodArgumentNotValidException ex) {

        log.error(writeLog(String.format(
            "400, Method argument is not valid. Details: %s", ex.getCause())));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getAllErrors().get(0).getDefaultMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(BadPayloadException.class)
    public ResponseEntity<ExceptionResponse> handle(BadPayloadException ex) {

        log.error(writeLog(
            String.format("400, invalid payload sent to publication service. Cause: %s", ex.getCause())
        ));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ExceptionResponse> handle(UnsupportedOperationException ex) {

        log.error(writeLog(String.format(
            "400, unsupported REST operation send to publication service. Cause: %s", ex.getCause())));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(ServiceToServiceException.class)
    public ResponseEntity<ExceptionResponse> handle(ServiceToServiceException ex) {
        log.error(writeLog(
            String.format("ServiceToServiceException was thrown with the init cause: %s", ex.getCause())
        ));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(exceptionResponse);
    }

    @ExceptionHandler(ThirdPartyServiceException.class)
    public ResponseEntity<ExceptionResponse> handle(ThirdPartyServiceException ex) {
        log.error(writeLog(ex.getMessage()));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode())).body(exceptionResponse);
    }

    @ExceptionHandler(CsvCreationException.class)
    public ResponseEntity<ExceptionResponse> handle(CsvCreationException ex) {
        log.error(writeLog(String.format("CsvCreationException was thrown with the init cause: %s", ex.getCause())));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(ExcelCreationException.class)
    public ResponseEntity<ExceptionResponse> handle(ExcelCreationException ex) {
        log.error(writeLog(String.format("ExcelCreationException was thrown with the init cause: %s", ex.getCause())));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }
}
