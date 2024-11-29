package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MediaApplicationReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MiDataReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.SystemAdminUpdateEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.UnidentifiedBlobEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.LocationSubscriptionDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class NotificationService {
    private final EmailService emailService;

    private final FileCreationService fileCreationService;

    private final DataManagementService dataManagementService;

    private final SubscriptionNotificationService subscriptionNotificationService;

    @Value("${notify.pi-team-email}")
    private String piTeamEmail;

    @Value("${file-retention-weeks}")
    private int fileRetentionWeeks;

    @Value("${env-name}")
    private String envName;

    @Autowired
    public NotificationService(EmailService emailService,
                               FileCreationService fileCreationService,
                               DataManagementService dataManagementService,
                               SubscriptionNotificationService subscriptionNotificationService) {
        this.emailService = emailService;
        this.fileCreationService = fileCreationService;
        this.dataManagementService = dataManagementService;
        this.subscriptionNotificationService = subscriptionNotificationService;
    }

    /**
     * Handles the incoming request for media applications reporting emails.
     * Creates a csv, builds and sends the email.
     *
     * @param mediaApplicationList The list of media applications to send in the email
     */
    public String handleMediaApplicationReportingRequest(List<MediaApplication> mediaApplicationList) {
        byte[] mediaApplicationsCsv = fileCreationService.createMediaApplicationReportingCsv(mediaApplicationList);
        EmailToSend email = emailService.handleEmailGeneration(
            new MediaApplicationReportingEmailData(piTeamEmail, mediaApplicationsCsv, fileRetentionWeeks, envName),
            Templates.MEDIA_APPLICATION_REPORTING_EMAIL
        );
        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    /**
     * This method handles the sending of the unidentified blobs email.
     *
     * @param noMatchArtefactList A list of no match artefacts.
     * @return The ID that references the unidentified blobs email.
     */
    public String unidentifiedBlobEmailRequest(List<NoMatchArtefact> noMatchArtefactList) {
        EmailToSend email = emailService.handleEmailGeneration(
            new UnidentifiedBlobEmailData(piTeamEmail, noMatchArtefactList, envName),
            Templates.BAD_BLOB_EMAIL
        );
        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    /**
     * Handles the incoming request for sending out email with MI data report.
     *
     * @return The ID that references the MI data reporting email.
     */
    public String handleMiDataForReporting() {
        byte[] excel;
        try {
            excel = fileCreationService.generateMiReport();
        } catch (IOException e) {
            log.warn(writeLog("Error generating excel file attachment"));
            throw new ExcelCreationException(e.getMessage());
        }

        EmailToSend email = emailService.handleEmailGeneration(
            new MiDataReportingEmailData(piTeamEmail, excel, fileRetentionWeeks, envName),
            Templates.MI_DATA_REPORTING_EMAIL
        );
        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    /**
     * This method handles the sending the email to all system admins for some actions on the application.
     *
     * @param body The body of the system admin update email.
     * @return The ID that references the system admin update email.
     */
    public String sendSystemAdminUpdateEmailRequest(SystemAdminAction body) {
        String referenceId = UUID.randomUUID().toString();
        List<EmailToSend> email = emailService.handleBatchEmailGeneration(
            new SystemAdminUpdateEmailData(body, envName, referenceId), Templates.SYSTEM_ADMIN_UPDATE_EMAIL
        );

        email.forEach(emailService::sendEmail);
        return referenceId;
    }

    /**
     * This method handles the bulk sending of subscription emails.
     *
     * @param bulkSubscriptionEmail The list of subscriptions that need to be fulfilled.
     * @return The ID that references the subscription notification email.
     */
    public String bulkSendSubscriptionEmail(BulkSubscriptionEmail bulkSubscriptionEmail) {
        Artefact artefact = dataManagementService.getArtefact(bulkSubscriptionEmail.getArtefactId());
        String locationName = dataManagementService.getLocation(artefact.getLocationId()).getName();
        String referenceId = UUID.randomUUID().toString();

        if (artefact.getIsFlatFile().equals(Boolean.TRUE)) {
            subscriptionNotificationService.flatFileBulkSubscriptionEmailRequest(
                bulkSubscriptionEmail, artefact, locationName, referenceId
            );
        } else {
            subscriptionNotificationService.rawDataBulkSubscriptionEmailRequest(
                bulkSubscriptionEmail, artefact, locationName, referenceId
            );
        }
        return referenceId;
    }

    /**
     * This method handles the sending the email to all the subscribers who are subscribe to a location.
     *
     * @param body The body of the location subscription notification email.
     * @return The ID that references the location subscription notification email.
     */
    public String sendDeleteLocationSubscriptionEmail(LocationSubscriptionDeletion body) {
        String referenceId = UUID.randomUUID().toString();
        List<EmailToSend> email = emailService.handleBatchEmailGeneration(
            new LocationSubscriptionDeletionEmailData(body, referenceId), Templates.DELETE_LOCATION_SUBSCRIPTION
        );

        email.forEach(emailService::sendEmail);
        return referenceId;
    }
}
