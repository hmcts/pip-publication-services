package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception that captures the message when a subscription is not found.
 */
public class PublicationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -2261417166948666798L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public PublicationNotFoundException(String message) {
        super(message);
    }

}
