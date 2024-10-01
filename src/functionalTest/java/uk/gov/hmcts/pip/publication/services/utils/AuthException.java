package uk.gov.hmcts.pip.publication.services.utils;

import java.io.Serial;

public class AuthException extends RuntimeException {
    private static final long serialVersionUID = -326686171637352006L;

    public AuthException(String error) {
        super(error);
    }
}
