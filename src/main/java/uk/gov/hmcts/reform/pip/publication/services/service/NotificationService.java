package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class NotificationService {
    @Autowired
    private EmailService emailService;

    @Autowired
    private FileCreationService fileCreationService;

    @Autowired
    private DataManagementService dataManagementService;

    /**
     * Handles the incoming request for welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and isExisting values e.g.
     *             {email: 'example@email.com', isExisting: 'true'}
     */
    public String handleWelcomeEmailRequest(WelcomeEmail body) {
        log.info(writeLog(String.format("Welcome email being processed for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        return emailService.sendEmail(emailService.buildWelcomeEmail(body, body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.MEDIA_NEW_ACCOUNT_SETUP.template)).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', forename: 'foo', surname: 'bar'}
     */
    public String azureNewUserEmailRequest(CreatedAdminWelcomeEmail body) {
        log.info(writeLog(String.format("New User Welcome email "
                                                   + "being processed for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        EmailToSend email = emailService.buildCreatedAdminWelcomeEmail(body,
                                                                       Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * Handles the incoming request for media applications reporting emails.
     * Creates a csv, builds and sends the email.
     *
     * @param mediaApplicationList The list of media applications to send in the email
     */
    public String handleMediaApplicationReportingRequest(List<MediaApplication> mediaApplicationList) {
        EmailToSend email = emailService.buildMediaApplicationReportingEmail(
            fileCreationService.createMediaApplicationReportingCsv(mediaApplicationList),
            Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template);

        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the subscription email, and forwarding on to the relevant email client.
     *
     * @param body The subscription message that is to be fulfilled.
     * @return The ID that references the subscription message.
     */
    public String subscriptionEmailRequest(SubscriptionEmail body) {
        log.info(writeLog(String.format("Sending subscription email for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        if (artefact.getIsFlatFile().equals(Boolean.TRUE)) {
            return emailService.sendEmail(emailService.buildFlatFileSubscriptionEmail(
                                                  body, artefact,
                                                  Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template))
                .getReference().orElse(null);
        } else {
            return emailService.sendEmail(emailService.buildRawDataSubscriptionEmail(
                body, artefact, Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template))
                .getReference().orElse(null);
        }
    }

    /**
     * Handles the incoming request for duplicate media account emails,
     * checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', fullname: 'foo bar'}
     */
    public String mediaDuplicateUserEmailRequest(DuplicatedMediaEmail body) {
        EmailToSend email = emailService.buildDuplicateMediaSetupEmail(body,
                                                                     Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the unidentified blobs email.
     *
     * @param noMatchArtefactList A list of no match artefacts.
     * @return The ID that references the unidentified blobs email.
     */
    public String unidentifiedBlobEmailRequest(List<NoMatchArtefact> noMatchArtefactList) {
        EmailToSend email = emailService
            .buildUnidentifiedBlobsEmail(noMatchArtefactList, Templates.BAD_BLOB_EMAIL.template);

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * This method handles the sending of the media user verification email.
     *
     * @param body The body of the media verification email.
     * @return The ID that references the media user verification email.
     */
    public String mediaUserVerificationEmailRequest(MediaVerificationEmail body) {
        EmailToSend email = emailService
            .buildMediaUserVerificationEmail(body, Templates.MEDIA_USER_VERIFICATION_EMAIL.template);

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * Handles the sending of the inactive user notification email.
     *
     * @param body The body of the inactive user notification email.
     * @return The ID that references the inactive user notification email.
     */
    public String inactiveUserNotificationEmailRequest(InactiveUserNotificationEmail body) {
        EmailToSend email;
        if ("PI_AAD".equals(body.getUserProvenance())) {
            email = emailService
                .buildInactiveUserNotificationEmail(body, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD.template);
        } else {
            email = emailService
                .buildInactiveUserNotificationEmail(body, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT.template);
        }

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for sending out email with MI data report.
     *
     * @return The ID that references the MI data reporting email.
     */
    public String handleMiDataForReporting() {
        EmailToSend email = emailService.buildMiDataReportingEmail(Templates.MI_DATA_REPORTING_EMAIL.template);
        return emailService.sendEmail(email).getReference().orElse(null);
    }

    public List<String> sendSystemAdminUpdateEmailRequest(SystemAdminAction body) {
        List<EmailToSend> email = emailService
            .buildSystemAdminUpdateEmail(body, Templates.SYSTEM_ADMIN_UPDATE_EMAIL.template);

        var sentEmails = new ArrayList<String>();
        email.forEach(emailToSend -> sentEmails.add(
            emailService.sendEmail(emailToSend).getReference().orElse(null)
        ));
        return sentEmails;
    }
}
