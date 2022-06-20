package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception for cross service calls within P&I failing.
 */
public class ServiceToServiceException extends RuntimeException {

    private static final long serialVersionUID = -3903002271082889318L;
    private static final String EXCEPTION_MESSAGE = "Request to %s failed due to: %s";

    public ServiceToServiceException(String serviceThatFailed, String message) {
        super(String.format(EXCEPTION_MESSAGE, serviceThatFailed, message));
    }
}
