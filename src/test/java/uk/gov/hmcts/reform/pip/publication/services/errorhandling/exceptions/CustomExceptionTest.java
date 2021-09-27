package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomExceptionTest {

    private static final String TEST_MESSAGE = "This is a test message";
    public static final String EXPECTED_MESSAGE = "The message should match the message passed in";

    @Test
    public void testCreationOfPublicationNotFoundException() {
        PublicationNotFoundException publicationNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, publicationNotFoundException.getMessage(),
                     EXPECTED_MESSAGE
        );
    }

    @Test
    public void testCreationOfBadPayloadException() {
        BadPayloadException badPayloadException
            = new BadPayloadException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, badPayloadException.getMessage(),
                     EXPECTED_MESSAGE);
    }

    @Test
    public void testCreationOfNotifyException() {
        NotifyException notifyException
            = new NotifyException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, notifyException.getMessage(),
                     EXPECTED_MESSAGE);
    }

}
