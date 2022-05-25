package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    static final String TEST_MESSAGE = "This is a test message";
    static final String STATUS_CODE_NOT_FOUND = "Status code should be not found";
    static final String RESPONSE_SHOULD_CONTAIN_BODY = "Response should contain a body";
    static final String ASSERT_MESSAGE = "The message should match the message passed in";

    @Test
    void testHandleSubscriptionNotFound() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(subscriptionNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), STATUS_CODE_NOT_FOUND);
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_BODY);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     ASSERT_MESSAGE);
    }

    @Test
    void testHandleBadPayload() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        BadPayloadException badPayloadException
            = new BadPayloadException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(badPayloadException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE_NOT_FOUND);
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_BODY);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     ASSERT_MESSAGE);
    }

    @Test
    void testHandleNotifyException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        NotifyException notifyException = new NotifyException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(notifyException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE_NOT_FOUND);
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_BODY);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     ASSERT_MESSAGE);
    }

    @Test
    void testHandleCsvCreationException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        CsvCreationException csvCreationException = new CsvCreationException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(csvCreationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE_NOT_FOUND);
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_BODY);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     ASSERT_MESSAGE);
    }
}
