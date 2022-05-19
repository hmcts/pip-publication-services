package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    static final String TEST_MESSAGE = "This is a test message";
    static final String STATUS_CODE = "Status code should be not found";
    static final String BODY_RESPONSE = "Response should contain a body";
    static final String PASSED_IN_MESSAGE = "The message should match the message passed in";

    @Test
    void testHandleSubscriptionNotFound() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(subscriptionNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }

    @Test
    void testHandleBadPayload() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        BadPayloadException badPayloadException
            = new BadPayloadException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(badPayloadException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }

    @Test
    void testHandleNotifyException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        NotifyException notifyException = new NotifyException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(notifyException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }

    @Test
    void testHandleUnsupportedOperationException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        UnsupportedOperationException unsupportedOperationException
            = new UnsupportedOperationException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(unsupportedOperationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }
}
