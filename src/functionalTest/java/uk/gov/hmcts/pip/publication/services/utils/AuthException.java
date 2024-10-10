package uk.gov.hmcts.pip.publication.services.utils;

public class AuthException extends RuntimeException {
    private static final long serialVersionUID = -326686171637352006L;

    public AuthException(String error) {
        super(error);
    }
}
