package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Exception for failed external api calls to third party recipients.
 */
public class ThirdPartyServiceException extends RuntimeException {

    private static final long serialVersionUID = -8090232438292245181L;

    private static final String MESSAGE = "Third party request to: %s failed after 3 retries due to: %s";
    private final int statusCodeResponse;

    public ThirdPartyServiceException(Throwable throwable, String api) {
        super(String.format(MESSAGE, api, throwable.getMessage()));
        WebClientResponseException mappedEx = (WebClientResponseException) throwable;
        statusCodeResponse = mappedEx.getStatusCode().value();
    }

    public int getStatusCode() {
        return statusCodeResponse;
    }
}
