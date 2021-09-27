package uk.gov.hmcts.reform.pip.publication_services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication_services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication_services.models.EmailToSend;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class EmailService {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";

    @Value("${notify.links.subscription-page-link}")
    private String subscriptionPageLink;

    @Value("${notify.links.start-page-link}")
    private String startPageLink;

    @Value("${notify.links.gov-guidance-page-link}")
    private String govGuidancePageLink;

    @Autowired
    EmailClient emailClient;

    public SendEmailResponse buildEmail(String email, String template) {
        EmailToSend emailToSend = generateEmail(email, template, buildWelcomePersonalisation());
        try {
            log.info("Sending email success. Reference ID: {}", emailToSend.getReferenceId());
            return emailClient.sendEmail(emailToSend.getTemplate(), emailToSend.getEmailAddress(),
                                         emailToSend.getPersonalisation(), emailToSend.getReferenceId());
        } catch (NotificationClientException e) {
            log.warn("Failed to send email. Reference ID: {}. Reason:", emailToSend.getReferenceId(), e);
            throw new NotifyException(e.getMessage());
        }
    }

    protected Map<String, String> buildWelcomePersonalisation() {
        Map<String, String> personalisation = new HashMap<>();

        personalisation.put(SUBSCRIPTION_PAGE_LINK, subscriptionPageLink);
        personalisation.put(START_PAGE_LINK, startPageLink);
        personalisation.put(GOV_GUIDANCE_PAGE_LINK, govGuidancePageLink);

        return personalisation;
    }

    private EmailToSend generateEmail(String email, String template, Map<String, String> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
    }
}
