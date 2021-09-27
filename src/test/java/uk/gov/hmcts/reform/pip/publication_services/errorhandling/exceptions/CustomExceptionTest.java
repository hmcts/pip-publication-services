package uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomExceptionTest {

    private static final String TEST_MESSAGE = "This is a test message";

    @Test
    public void testCreationOfPublicationNotFoundException() {
        PublicationNotFoundException publicationNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, publicationNotFoundException.getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    public void testCreationOfBadPayloadException() {
        BadPayloadException badPayloadException
            = new BadPayloadException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, badPayloadException.getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    public void testCreationOfTemplateNotFoundException() {
        TemplateNotFoundException templateNotFoundException
            = new TemplateNotFoundException();
        assertEquals("No template was found", templateNotFoundException.getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    public void testCreationOfNotifyException() {
        NotifyException notifyException
            = new NotifyException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, notifyException.getMessage(),
                     "The message should match the message passed in");
    }

}
