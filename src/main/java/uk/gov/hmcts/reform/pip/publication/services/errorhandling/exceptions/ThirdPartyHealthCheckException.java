package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

public class ThirdPartyHealthCheckException extends RuntimeException {
    private static final long serialVersionUID = -4926683933017391420L;

    public ThirdPartyHealthCheckException(String message) {
        super(message);
    }
}
