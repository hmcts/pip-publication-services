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
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;
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

    @Autowired
    private RateLimitingService rateLimitingService;

    @Value("${notify.pi-team-email}")
    private String piTeamEmail;

    protected EmailToSend buildWelcomeEmail(WelcomeEmail body, Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildWelcomePersonalisation(body));
    }

    protected EmailToSend buildCreatedAdminWelcomeEmail(CreatedAdminWelcomeEmail body, Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildAdminAccountPersonalisation(body));
    }

    protected EmailToSend buildFlatFileSubscriptionEmail(SubscriptionEmail body, Artefact artefact,
                                                         Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildFlatFileSubscriptionPersonalisation(body, artefact));
    }

    protected EmailToSend buildRawDataSubscriptionEmail(SubscriptionEmail body, Artefact artefact,
                                                        Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildRawDataSubscriptionPersonalisation(body, artefact));
    }

    protected EmailToSend buildDuplicateMediaSetupEmail(DuplicatedMediaEmail body, Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildDuplicateMediaAccountPersonalisation(body));
    }

    protected EmailToSend buildMediaApplicationReportingEmail(byte[] csvMediaApplications, Templates emailTemplate) {
        rateLimitingService.validate(piTeamEmail, emailTemplate.getEmailLimit());
        return generateEmail(piTeamEmail, emailTemplate.getTemplate(),
                             personalisationService
                                 .buildMediaApplicationsReportingPersonalisation(csvMediaApplications));
    }

    protected EmailToSend buildUnidentifiedBlobsEmail(List<NoMatchArtefact> noMatchArtefactList,
                                                      Templates emailTemplate) {
        rateLimitingService.validate(piTeamEmail, emailTemplate.getEmailLimit());
        return generateEmail(piTeamEmail, emailTemplate.getTemplate(),
                             personalisationService.buildUnidentifiedBlobsPersonalisation(noMatchArtefactList));
    }

    protected EmailToSend buildMediaUserVerificationEmail(MediaVerificationEmail body,Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildMediaVerificationPersonalisation(body));
    }

    protected EmailToSend buildMediaApplicationRejectionEmail(MediaRejectionEmail body, Templates emailTemplate)
        throws IOException {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildMediaRejectionPersonalisation(body));
    }

    protected EmailToSend buildInactiveUserNotificationEmail(InactiveUserNotificationEmail body,
                                                             Templates emailTemplate) {
        rateLimitingService.validate(body.getEmail(), emailTemplate.getEmailLimit());
        return generateEmail(body.getEmail(), emailTemplate.getTemplate(),
                             personalisationService.buildInactiveUserNotificationPersonalisation(body));
    }

    protected List<EmailToSend> buildSystemAdminUpdateEmail(SystemAdminAction body, Templates emailTemplate) {
        List<String> emails = applyEmailRateLimit(body.getEmailList(), emailTemplate.getEmailLimit());
        return generateEmail(emails, emailTemplate.getTemplate(),
                             personalisationService.buildSystemAdminUpdateEmailPersonalisation(body));
    }

    protected EmailToSend buildMiDataReportingEmail(Templates emailTemplate) {
        rateLimitingService.validate(piTeamEmail, emailTemplate.getEmailLimit());
        return generateEmail(piTeamEmail, emailTemplate.getTemplate(),
                             personalisationService.buildMiDataReportingPersonalisation());
    }

    protected List<EmailToSend> buildDeleteLocationSubscriptionEmail(
        LocationSubscriptionDeletion body, Templates emailTemplate) {
        List<String> emails = applyEmailRateLimit(body.getSubscriberEmails(), emailTemplate.getEmailLimit());
        return generateEmail(emails, emailTemplate.getTemplate(),
                             personalisationService.buildDeleteLocationSubscriptionEmailPersonalisation(body));
    }

    private List<String> applyEmailRateLimit(List<String> emails, EmailLimit emailLimit) {
        return emails.stream()
            .filter(e -> rateLimitingService.isValid(e, emailLimit))
            .toList();
    }

    private List<EmailToSend> generateEmail(List<String> emails, String template,
                                            Map<String, Object> personalisation) {
        List<EmailToSend> createdEmails = new ArrayList<>();

        for (String email : emails) {
            String referenceId = UUID.randomUUID().toString();
            createdEmails.add(new EmailToSend(email, template, personalisation, referenceId));
        }

        return createdEmails;
    }

    private EmailToSend generateEmail(String email, String template, Map<String, Object> personalisation) {
        String referenceId = UUID.randomUUID().toString();
        return new EmailToSend(email, template, personalisation, referenceId);
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
}
