package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.Data;

@Data
public class PersonalisationLinks {

    private String subscriptionPageLink;
    private String startPageLink;
    private String govGuidancePageLink;
    private String aadSignInPageLink;
    private String aadAdminSignInPageLink;
    private String aadPwResetLinkAdmin;
    private String aadPwResetLinkMedia;
    private String mediaVerificationPageLink;
    private String adminDashboardLink;
    private String cftSignInPageLink;
}
