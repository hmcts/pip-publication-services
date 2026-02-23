package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class AzureSecretReadException extends RuntimeException {

    private static final long serialVersionUID = 3052325938538028105L;

    public AzureSecretReadException(String message) {
        super(message);
    }
}
