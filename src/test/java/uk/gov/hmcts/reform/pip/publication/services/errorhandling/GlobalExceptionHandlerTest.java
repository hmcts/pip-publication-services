package uk.gov.hmcts.reform.pip.publication.services.errorhandling;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.PublicationNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    static final String TEST_MESSAGE = "This is a test message";
    static final String STATUS_CODE = "Status code should be not found";
    static final String BODY_RESPONSE = "Response should contain a body";
    static final String PASSED_IN_MESSAGE = "The message should match the message passed in";

    @Mock
    MethodArgumentNotValidException methodArgumentNotValidException;

    @BeforeAll
    public static void setup() {
        MockitoAnnotations.openMocks(MethodArgumentNotValidException.class);
    }

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

    @Test
    void testMethodArgumentTypeMismatchException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        when(methodArgumentNotValidException.getAllErrors()).thenReturn(List.of(
            new ObjectError("Subscription", "Error message")));

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(methodArgumentNotValidException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), STATUS_CODE);
        assertNotNull(responseEntity.getBody(), BODY_RESPONSE);

        assertTrue(responseEntity.getBody().getMessage().contains("Error message"),
                   "Body does not contain error message");

    }
}
