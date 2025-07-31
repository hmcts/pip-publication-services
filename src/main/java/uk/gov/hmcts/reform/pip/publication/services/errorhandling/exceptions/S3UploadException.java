package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class S3UploadException extends RuntimeException {
    private static final long serialVersionUID = -3060840161643189023L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     * @param cause The cause exception return
     */
    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
