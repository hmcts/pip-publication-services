package uk.gov.hmcts.reform.pip.publication.services.models.request;

/**
 * Enum that highlights the available types for the subscription object being passed in.
 */
public enum SubscriptionTypes {
    CASE_NUMBER,
    @Deprecated CASE_URN,
    CASE_NAME,
    LOCATION_ID;
}
