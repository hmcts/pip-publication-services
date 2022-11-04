package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class ExcelCreationException extends RuntimeException {
    private static final long serialVersionUID = -3060840161643189023L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public ExcelCreationException(String message) {
        super(message);
    }
}
