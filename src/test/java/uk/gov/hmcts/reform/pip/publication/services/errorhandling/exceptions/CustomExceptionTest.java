package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomExceptionTest {

    private static final String TEST_MESSAGE = "This is a test message";
    private static final String EXPECTED_MESSAGE = "The message should match the message passed in";

    @Test
    void testCreationOfPublicationNotFoundException() {
        PublicationNotFoundException publicationNotFoundException
            = new PublicationNotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, publicationNotFoundException.getMessage(),
                     EXPECTED_MESSAGE
        );
    }

    @Test
    void testCreationOfBadPayloadException() {
        BadPayloadException badPayloadException
            = new BadPayloadException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, badPayloadException.getMessage(),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testCreationOfNotifyException() {
        NotifyException notifyException
            = new NotifyException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, notifyException.getMessage(),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testCreationOfServiceToServiceException() {
        ServiceToServiceException exception = new ServiceToServiceException("Test service", TEST_MESSAGE);
        assertEquals("Request to Test service failed due to: " + TEST_MESSAGE, exception.getMessage(),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testCreationOfThirdPartyServiceException() {
        ThirdPartyServiceException exception = new ThirdPartyServiceException(
            new WebClientResponseException(404, TEST_MESSAGE, null, null, null), "testApi");
        assertEquals("Third party request to: testApi failed after 3 retries due to: 404 " + TEST_MESSAGE,
                     exception.getMessage(), EXPECTED_MESSAGE);

    }

    @Test
    void testCreationOfCsvCreationException() {
        CsvCreationException csvCreationException = new CsvCreationException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, csvCreationException.getMessage(), EXPECTED_MESSAGE);
    }
}
