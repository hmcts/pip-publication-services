package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class TooManyEmailsException extends RuntimeException {

    private static final long serialVersionUID = -26604681407775216L;

    public TooManyEmailsException(String message) {
        super(message);
    }
}
