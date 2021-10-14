package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception that captures issues with Notify.
 */
public class NotifyException extends RuntimeException {

    private static final long serialVersionUID = 174229409261569993L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public NotifyException(String message) {
        super(message);
    }
}
