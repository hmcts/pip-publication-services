package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Component
@Slf4j
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.TooManyMethods"})
public class EmailService {
    @Autowired
    EmailClient emailClient;

    @Autowired
    PersonalisationService personalisationService;

    @Value("${notify.pi-team-email}")
    private String piTeamEmail;

    protected EmailToSend buildWelcomeEmail(WelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, personalisationService.buildWelcomePersonalisation(body));
    }

    protected EmailToSend buildCreatedAdminWelcomeEmail(CreatedAdminWelcomeEmail body, String template) {
        return generateEmail(body.getEmail(), template, personalisationService.buildAdminAccountPersonalisation(body));
    }

    protected EmailToSend buildFlatFileSubscriptionEmail(SubscriptionEmail body, Artefact artefact,
                                                         String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildFlatFileSubscriptionPersonalisation(body, artefact));
    }

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

    protected EmailToSend buildUnidentifiedBlobsEmail(List<NoMatchArtefact> noMatchArtefactList, String template) {
        return generateEmail(piTeamEmail, template,
                             personalisationService
                                .buildUnidentifiedBlobsPersonalisation(noMatchArtefactList));
    }

    protected EmailToSend buildMediaUserVerificationEmail(MediaVerificationEmail body, String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildMediaVerificationPersonalisation(body));
    }

    protected EmailToSend buildMediaApplicationRejectionEmail(MediaRejectionEmail body, String template)
        throws IOException {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildMediaRejectionPersonalisation(body));
    }

    protected EmailToSend buildInactiveUserNotificationEmail(InactiveUserNotificationEmail body, String template) {
        return generateEmail(body.getEmail(), template,
                             personalisationService.buildInactiveUserNotificationPersonalisation(body));
    }

    protected List<EmailToSend> buildSystemAdminUpdateEmail(SystemAdminAction body, String template) {
        return generateEmail(body.getEmailList(), template,
                             personalisationService.buildSystemAdminUpdateEmailPersonalisation(body));
    }

    private List<EmailToSend> generateEmail(List<String> email, String template, Map<String, Object> personalisation) {

        var createdEmails = new ArrayList<EmailToSend>();

        for (String notifyEmail : email) {
            String referenceId = UUID.randomUUID().toString();
            createdEmails.add(new EmailToSend(notifyEmail, template, personalisation, referenceId));
        }

        return createdEmails;
    }

    private EmailToSend generateEmail(String email, String template, Map<String, Object> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
    }

    protected EmailToSend buildMiDataReportingEmail(String template) {
        return generateEmail(piTeamEmail, template,
                             personalisationService.buildMiDataReportingPersonalisation());
    }

    public SendEmailResponse sendEmail(EmailToSend emailToSend) {
        try {
            SendEmailResponse response = emailClient.sendEmail(emailToSend.getTemplate(), emailToSend.getEmailAddress(),
                                         emailToSend.getPersonalisation(), emailToSend.getReferenceId());
            String emailDescription = Templates.get(emailToSend.getTemplate()).getDescription();
            log.info(writeLog(
                String.format("%s successfully sent with reference ID: %s",
                              emailDescription, emailToSend.getReferenceId())
            ));
            return response;
        } catch (NotificationClientException e) {
            log.error(writeLog(String.format("Failed to send email. "
                                                       + "Reference ID: %s. "
                                                       + "Reason: %s", emailToSend.getReferenceId(), e)));
            throw new NotifyException(e.getMessage());
        }
    }

    protected List<EmailToSend> buildDeleteLocationSubscriptionEmail(
        LocationSubscriptionDeletion body, String template) {
        return generateEmail(body.getSubscriberEmails(), template,
                             personalisationService.buildDeleteLocationSubscriptionEmailPersonalisation(body));
    }
}
