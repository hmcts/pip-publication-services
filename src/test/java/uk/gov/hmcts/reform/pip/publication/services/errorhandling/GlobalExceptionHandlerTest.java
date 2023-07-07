package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String MESSAGES_MATCH = "The message should match the message passed in";
    private static final String RESPONSE_BODY_MESSAGE = "Response should contain a body";

    static final String TEST_MESSAGE = "This is a test message";
    static final String STATUS_CODE = "Status code should be not found";
    static final String BODY_RESPONSE = "Response should contain a body";
    static final String PASSED_IN_MESSAGE = "The message should match the message passed in";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Test
    void testHandleSubscriptionNotFound() {
        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(subscriptionNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_BODY_MESSAGE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     MESSAGES_MATCH);
    }

    @Test
    void testHandleBadPayload() {
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
        NotifyException notifyException = new NotifyException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(notifyException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_BODY_MESSAGE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     MESSAGES_MATCH);
    }

    @Test
    void testHandleServiceToServiceException() {
        ServiceToServiceException exception = new ServiceToServiceException("Test service", TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode(), "Status code should be bad gateway");
        assertNotNull(responseEntity.getBody(), RESPONSE_BODY_MESSAGE);
        assertEquals("Request to Test service failed due to: " +  TEST_MESSAGE,
                     responseEntity.getBody().getMessage(), MESSAGES_MATCH);
    }

    @Test
    void testHandleUnsupportedOperationException() {
        UnsupportedOperationException unsupportedOperationException
            = new UnsupportedOperationException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(unsupportedOperationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }

    @Test
    void testMethodArgumentTypeMismatchException() {
        when(methodArgumentNotValidException.getAllErrors()).thenReturn(List.of(
            new ObjectError("Subscription", "Error message")));

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(methodArgumentNotValidException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);

        assertTrue(responseEntity.getBody().getMessage().contains("Error message"),
                   "Body does not contain error message");

    }

    @Test
    void testThirdPartyServiceException() {
        ThirdPartyServiceException exception = new ThirdPartyServiceException(
            new WebClientResponseException(404, TEST_MESSAGE, null, null, null), "testApi");

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(exception);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals("Third party request to: testApi failed after 3 retries due to: 404 This is a test message",
                     responseEntity.getBody().getMessage(), MESSAGES_MATCH
        );
    }

    @Test
    void testHandleCsvCreationException() {
        CsvCreationException csvCreationException = new CsvCreationException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(csvCreationException);


        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }

    @Test
    void testHandleExcelCreationException() {
        ExcelCreationException excelCreationException = new ExcelCreationException(TEST_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(excelCreationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);
        assertEquals(TEST_MESSAGE, responseEntity.getBody().getMessage(),
                     PASSED_IN_MESSAGE);
    }
}
