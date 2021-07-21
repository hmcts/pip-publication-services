package uk.gov.hmcts.reform.rsecheck.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import uk.gov.hmcts.reform.demo.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.demo.errorhandling.GlobalExceptionHandler;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.PublicationNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    @Test
    public void testHandleSubscriptionNotFoundMethod() {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException("This is a test message");

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handlePublicationNotFound(subscriptionNotFoundException, servletWebRequest);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("This is a test message", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }
}
