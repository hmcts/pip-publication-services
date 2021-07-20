package uk.gov.hmcts.reform.rsecheck.errorhandling.exceptions;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.PublicationNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationNotFoundExceptionTest {

    @Test
    public void testCreationOfPublicationNotFoundException() {

        PublicationNotFoundException subscriptionNotFoundException
            = new PublicationNotFoundException("This is a test message");
        assertEquals("This is a test message", subscriptionNotFoundException.getMessage(),
                     "The message should match the message passed in");

    }

}
