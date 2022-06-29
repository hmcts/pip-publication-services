package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception that captures issues with creating a csv.
 */
public class CsvCreationException extends RuntimeException {

    private static final long serialVersionUID = 3680025755666731772L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public CsvCreationException(String message) {
        super(message);
    }
}
