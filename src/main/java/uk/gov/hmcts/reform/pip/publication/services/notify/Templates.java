package uk.gov.hmcts.reform.pip.publication.services.notify;

/**
 * Enum that contains all the templates that we use on GovNotify.
 */
public enum Templates {
    NEW_USER_WELCOME_EMAIL("b708c2dc-5794-4468-a8bf-f798fe1f91bc"),
    EXISTING_USER_WELCOME_EMAIL("321cbaa6-2a19-4980-87c6-fe90516db59b"),
    ADMIN_ACCOUNT_CREATION_EMAIL("77a658c5-7c69-4dcc-984e-214a3c37cea2");

    public final String template;

    /**
     * Constructor for templates enum that can handle a String with the template ID.
     * @param template the TemplateId from GovNotify
     */
    Templates(String template) {
        this.template = template;
    }
}
