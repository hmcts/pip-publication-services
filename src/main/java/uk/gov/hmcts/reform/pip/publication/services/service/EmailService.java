package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class EmailService {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String SURNAME = "surname";
    private static final String FORENAME = "first_name";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String YES = "Yes";
    private static final String NO = "No";

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @Autowired
    EmailClient emailClient;


    protected EmailToSend buildWelcomeEmail(WelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, buildWelcomePersonalisation());
    }

    protected EmailToSend buildCreatedAdminWelcomeEmail(CreatedAdminWelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, buildAdminAccountPersonalisation(body));
    }

    protected EmailToSend buildFlatFileSubscriptionEmail(SubscriptionEmail body, String template) {
        return generateEmail(body.getEmail(), template, buildFlatFileSubscriptionPersonalisation(body));
    }

    protected EmailToSend buildRawDataSubscriptionEmail(SubscriptionEmail body, String template) {
        return generateEmail(body.getEmail(), template, buildRawDataSubscriptionPersonalisation(body));

    }

    public EmailToSend generateEmail(String email, String template, Map<String, String> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
    }

    public SendEmailResponse sendEmail(EmailToSend emailToSend) {
        try {
            log.info("Sending email success. Reference ID: {}", emailToSend.getReferenceId());
            return emailClient.sendEmail(emailToSend.getTemplate(), emailToSend.getEmailAddress(),
                                         emailToSend.getPersonalisation(), emailToSend.getReferenceId()
            );
        } catch (NotificationClientException e) {
            log.warn("Failed to send email. Reference ID: {}. Reason:", emailToSend.getReferenceId(), e);
            throw new NotifyException(e.getMessage());
        }
    }

    private Map<String, String> buildWelcomePersonalisation() {
        Map<String, String> personalisation = new ConcurrentHashMap<>();
        personalisation.put(SUBSCRIPTION_PAGE_LINK, notifyConfigProperties.getLinks().getSubscriptionPageLink());
        personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());
        personalisation.put(GOV_GUIDANCE_PAGE_LINK, notifyConfigProperties.getLinks().getGovGuidancePageLink());
        return personalisation;
    }

    private Map<String, String> buildAdminAccountPersonalisation(CreatedAdminWelcomeEmail body) {
        Map<String, String> personalisation = new ConcurrentHashMap<>();
        personalisation.put(SURNAME, body.getSurname());
        personalisation.put(FORENAME, body.getForename());
        personalisation.put(AAD_RESET_LINK, notifyConfigProperties.getLinks().getAadPwResetLink());
        personalisation.put(AAD_SIGN_IN_LINK, notifyConfigProperties.getLinks().getAadSignInPageLink());
        return personalisation;
    }

    private Map<String, String> buildRawDataSubscriptionPersonalisation(SubscriptionEmail body) {
        Map<String, String> personalisation = new ConcurrentHashMap<>();
        body.getSubscriptions().entrySet().forEach(entry -> {
            switch (entry.getKey()) {
                case CASE_NUMBER:
                    personalisation.put(DISPLAY_CASE_NUMBERS, entry.getValue().size() > 0 ? YES : NO);
                    personalisation.put(CASE_NUMBERS, entry.getValue().toString());
                    break;
                case CASE_URN:
                    personalisation.put(DISPLAY_CASE_URN, entry.getValue().size() > 0 ? YES : NO);
                    personalisation.put(CASE_URN, entry.getValue().toString());
                    break;
                case LOCATION_ID:
                    personalisation.put(DISPLAY_LOCATIONS, entry.getValue().size() > 0 ? YES : NO);
                    personalisation.put(LOCATIONS, entry.getValue().toString());
                    break;
            }
        });

        return personalisation;
    }

    private Map<String, String> buildFlatFileSubscriptionPersonalisation(SubscriptionEmail body) {

    }
}
