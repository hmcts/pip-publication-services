package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.AzureSecretReadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;

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

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(NotifyException.class)
    public ResponseEntity<ExceptionResponse> handle(NotifyException ex) {

        log.error(writeLog(String.format(
            "400, Error while communicating with the notify service with reason %s", ex.getMessage())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handle(MethodArgumentNotValidException ex) {

        log.error(writeLog(String.format(
            "400, Method argument is not valid. Details: %s", ex.getCause())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getAllErrors().get(0).getDefaultMessage()));
    }

    @ExceptionHandler(BadPayloadException.class)
    public ResponseEntity<ExceptionResponse> handle(BadPayloadException ex) {

        log.error(writeLog(
            String.format("400, invalid payload sent to publication service. Cause: %s", ex.getCause())
        ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ExceptionResponse> handle(UnsupportedOperationException ex) {

        log.error(writeLog(String.format(
            "400, unsupported REST operation send to publication service. Cause: %s", ex.getCause())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(ServiceToServiceException.class)
    public ResponseEntity<ExceptionResponse> handle(ServiceToServiceException ex) {
        log.error(writeLog(
            String.format("ServiceToServiceException was thrown with the init cause: %s", ex.getCause())
        ));

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(ThirdPartyServiceException.class)
    public ResponseEntity<ExceptionResponse> handle(ThirdPartyServiceException ex) {
        log.error(writeLog(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode()))
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(CsvCreationException.class)
    public ResponseEntity<ExceptionResponse> handle(CsvCreationException ex) {
        log.error(writeLog(String.format("CsvCreationException was thrown with the init cause: %s", ex.getCause())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(ExcelCreationException.class)
    public ResponseEntity<ExceptionResponse> handle(ExcelCreationException ex) {
        log.error(writeLog(String.format("ExcelCreationException was thrown with the init cause: %s", ex.getCause())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(TooManyEmailsException.class)
    public ResponseEntity<ExceptionResponse> handle(TooManyEmailsException ex) {
        log.error(writeLog(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(AzureSecretReadException.class)
    public ResponseEntity<ExceptionResponse> handle(AzureSecretReadException ex) {
        log.error(writeLog(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        exceptionResponse.setTimestamp(LocalDateTime.now());
        return exceptionResponse;
    }
}
