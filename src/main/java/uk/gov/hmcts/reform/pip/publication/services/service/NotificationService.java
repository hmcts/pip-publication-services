package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
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

    /**
     * This method handles the sending the email to all the subscribers who are subscribe to a location.
     *
     * @param body The body of the location subscription notification email.
     * @return The ID that references the location subscription notification email.
     */
    public List<String> sendDeleteLocationSubscriptionEmail(LocationSubscriptionDeletion body) {
        List<EmailToSend> email = emailService
            .buildDeleteLocationSubscriptionEmail(body, Templates.DELETE_LOCATION_SUBSCRIPTION.template);

        var sentEmails = new ArrayList<String>();
        email.forEach(emailToSend ->
            sentEmails.add(emailService.sendEmail(emailToSend).getReference().orElse(null))
        );
        return sentEmails;
    }
}
