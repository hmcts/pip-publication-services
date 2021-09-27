package uk.gov.hmcts.reform.pip.publication.services.notify;


public enum Templates {
    NEW_USER_WELCOME_EMAIL("b708c2dc-5794-4468-a8bf-f798fe1f91bc"),
    EXISTING_USER_WELCOME_EMAIL("321cbaa6-2a19-4980-87c6-fe90516db59b");

    public final String template;

    Templates(String template) {
        this.template = template;
    }
}
