package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class S3UploadException extends RuntimeException {
    private static final long serialVersionUID = -3060840161643189023L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public S3UploadException(String message) {
        super(message);
    }
}
