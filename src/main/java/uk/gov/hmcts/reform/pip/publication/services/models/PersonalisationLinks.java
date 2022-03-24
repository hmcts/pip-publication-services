package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.Data;

@Data
public class PersonalisationLinks {

    private String subscriptionPageLink;
    private String startPageLink;
    private String govGuidancePageLink;
    private String aadSignInPageLink;
    private String aadPwResetLink;
}
