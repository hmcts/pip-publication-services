package uk.gov.hmcts.reform.pip.publication_services.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.PublicationNotFoundException;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.TemplateNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    @Test
    public void testHandleSubscriptionNotFound() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException("This is a test message");

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(subscriptionNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("This is a test message", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    public void testHandleBadPayload() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        BadPayloadException badPayloadException
            = new BadPayloadException("This is a test message");

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(badPayloadException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("This is a test message", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    public void testHandleNotifyException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        NotifyException notifyException = new NotifyException("This is a test message");

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(notifyException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("This is a test message", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }
}
