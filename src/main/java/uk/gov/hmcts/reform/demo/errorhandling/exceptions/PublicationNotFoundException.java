package uk.gov.hmcts.reform.demo.errorhandling.exceptions;

/**
 * Exception that captures the message when a subscription is not found.
 */
public class PublicationNotFoundException extends RuntimeException {

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public PublicationNotFoundException(String message) {
        super(message);
    }

}
