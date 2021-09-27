package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception when request body payload doesn't contain required json fields.
 */
public class BadPayloadException extends RuntimeException {

    private static final long serialVersionUID = -8763405687510342509L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public BadPayloadException(String message) {
        super(message);
    }
}
