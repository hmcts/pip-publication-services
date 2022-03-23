package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.AadWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
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
    private static final String AAD_SIGNIN_LINK = "https://pib2csbox.b2clogin.com/pib2csbox.onmicrosoft.com/oauth2/v2"
        + ".0/authorize?p=B2C_1_SignInUserFlow&client_id=c7e6e2c6-c23c-48e8-b9f4-6bad25a95331&nonce=defaultNonce"
        + "&redirect_uri=https%3A%2F%2Fpip-frontend.staging.platform.hmcts.net%2Flogin%2Freturn&scope=openid"
        + "&response_type=id_token&prompt=login";
    private static final String AAD_RESET_LINK = "https://pib2csbox.b2clogin.com/pib2csbox.onmicrosoft"
        + ".com/oauth2/v2.0/authorize?p=B2C_1_ResetTest&client_id=c7e6e2c6-c23c-48e8-b9f4-6bad25a95331"
        + "&nonce=defaultNonce&redirect_uri=https%3A%2F%2Fpip-frontend.staging.platform.hmcts.net%2Flogin%2Freturn"
        + "&scope=openid&response_type=id_token&prompt=login";
    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @Autowired
    EmailClient emailClient;

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

    protected EmailToSend buildWelcomeEmail(WelcomeEmail body) {
        return generateEmail(body.getEmail(), body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.NEW_USER_WELCOME_EMAIL.template, buildWelcomePersonalisation());
    }


    protected Map<String, String> buildWelcomePersonalisation() {
        Map<String, String> personalisation = new ConcurrentHashMap<>();
        personalisation.put(SUBSCRIPTION_PAGE_LINK, notifyConfigProperties.getLinks().getSubscriptionPageLink());
        personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());
        personalisation.put(GOV_GUIDANCE_PAGE_LINK, notifyConfigProperties.getLinks().getGovGuidancePageLink());
        return personalisation;
    }

    protected Map<String, String> buildAadPersonalisation(AadWelcomeEmail body) {
        Map<String, String> personalisation = new ConcurrentHashMap<>();
        personalisation.put("surname", body.getSurname());
        personalisation.put("first_name", body.getForename());
        personalisation.put("reset_password_link", AAD_RESET_LINK);
        personalisation.put("sign_in_page_link", AAD_SIGNIN_LINK);
        return personalisation;
    }

    protected EmailToSend buildAadWelcomeEmail(AadWelcomeEmail body) {
        return generateEmail(body.getEmail(), Templates.NEW_AZURE_USER_WELCOME_EMAIL.template,
                             buildAadPersonalisation(body)
        );
    }

    public EmailToSend generateEmail(String email, String template, Map<String, String> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
    }
}
