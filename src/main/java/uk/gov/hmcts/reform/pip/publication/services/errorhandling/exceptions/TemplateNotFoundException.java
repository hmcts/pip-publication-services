package uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions;

/**
 * Exception that notifies when a template is not found.
 */
public class TemplateNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -8191860207656076186L;
    private static final String INVALID_TEMPLATE_NAME = "No template was found";

    /**
     * Constructor for the Exception.
     */
    public TemplateNotFoundException() {
        super(INVALID_TEMPLATE_NAME);
    }
}
