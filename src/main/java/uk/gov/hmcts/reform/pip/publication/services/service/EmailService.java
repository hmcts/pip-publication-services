package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class EmailService {
    @Autowired
    EmailClient emailClient;

    @Autowired
    PersonalisationService personalisationService;

    @Value("${notify.pi-team-email}")
    private String piTeamEmail;

    protected EmailToSend buildWelcomeEmail(WelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, personalisationService.buildWelcomePersonalisation());
    }

    protected EmailToSend buildCreatedAdminWelcomeEmail(CreatedAdminWelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, personalisationService.buildAdminAccountPersonalisation(body));
    }

    protected EmailToSend buildFlatFileSubscriptionEmail(SubscriptionEmail body, Artefact artefact,
                                                         String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildFlatFileSubscriptionPersonalisation(body, artefact));
    }

    //TODO: This method is provided as a placeholder for now, and will be updated once JSON tickets have been played
    protected EmailToSend buildRawDataSubscriptionEmail(SubscriptionEmail body, Artefact artefact,
                                                        String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildRawDataSubscriptionPersonalisation(body, artefact));

    }

    protected EmailToSend buildDuplicateMediaSetupEmail(DuplicatedMediaEmail body, String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildDuplicateMediaAccountPersonalisation(body));
    }

    protected EmailToSend buildMediaApplicationReportingEmail(byte[] csvMediaApplications, String template) {
        return generateEmail(piTeamEmail, template,
                             personalisationService
                                .buildMediaApplicationsReportingPersonalisation(csvMediaApplications));
    }

    protected EmailToSend buildUnidentifiedBlobsEmail(Map<String, String> locationMap, String template) {
        return generateEmail(piTeamEmail, template,
                             personalisationService
                                .buildUnidentifiedBlobsPersonalisation(locationMap));
    }

    public EmailToSend generateEmail(String email, String template, Map<String, Object> personalisation) {
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
}
