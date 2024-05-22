package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SingleSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.Base64;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class SubscriptionNotificationService {
    private final EmailService emailService;
    private final DataManagementService dataManagementService;
    private final ChannelManagementService channelManagementService;

    @Value("${payload.json.max-size-in-kb}")
    private int maxPayloadSize;

    @Value("${file-retention-weeks}")
    private int fileRetentionWeeks;

    @Autowired
    public SubscriptionNotificationService(EmailService emailService, DataManagementService dataManagementService,
                                           ChannelManagementService channelManagementService) {
        this.emailService = emailService;
        this.dataManagementService = dataManagementService;
        this.channelManagementService = channelManagementService;
    }

    /**
     * This method handles the sending of the subscription email, and forwarding on to the relevant email client.
     *
     * @param body The subscription message that is to be fulfilled.
     * @return The ID that references the subscription message.
     */
    @Deprecated
    public String subscriptionEmailRequest(SingleSubscriptionEmail body) {
        log.info(writeLog(String.format("Sending subscription email for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        String locationName = dataManagementService.getLocation(artefact.getLocationId()).getName();

        if (artefact.getIsFlatFile().equals(Boolean.TRUE)) {
            byte[] flatFileData = dataManagementService.getArtefactFlatFile(artefact.getArtefactId());
            return flatFileSubscriptionEmailRequest(body, artefact, flatFileData, locationName);
        } else {
            String artefactSummary = getArtefactSummary(artefact);
            byte[] pdf = getFileBytes(artefact, FileType.PDF, false);
            boolean hasAdditionalPdf = artefact.getListType().hasAdditionalPdf()
                && artefact.getLanguage() != Language.ENGLISH;
            byte[] additionalPdf = hasAdditionalPdf ? getFileBytes(artefact, FileType.PDF, true)
                : new byte[0];
            byte[] excel = artefact.getListType().hasExcel() ? getFileBytes(artefact, FileType.EXCEL, false)
                : new byte[0];
            return rawDataSubscriptionEmailRequest(body, artefact, artefactSummary, pdf,
                                                   additionalPdf, excel, locationName);
        }
    }

    /**
     * This method handles the bulk sending of subscription emails.
     *
     * @param bulkSubscriptionEmail The list of subscriptions that need to be fulfilled.
     */
    @Async
    public void bulkSendSubscriptionEmail(BulkSubscriptionEmail bulkSubscriptionEmail) {
        Artefact artefact = dataManagementService.getArtefact(bulkSubscriptionEmail.getArtefactId());
        String locationName = dataManagementService.getLocation(artefact.getLocationId()).getName();

        if (artefact.getIsFlatFile().equals(Boolean.TRUE)) {
            byte[] flatFileData = dataManagementService.getArtefactFlatFile(artefact.getArtefactId());

            bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                                EmailHelper.maskEmail(subscriptionEmail.getEmail()))));

                flatFileSubscriptionEmailRequest(subscriptionEmail, artefact, flatFileData, locationName);
            });
        } else {
            String artefactSummary = getArtefactSummary(artefact);
            byte[] pdf = getFileBytes(artefact, FileType.PDF, false);
            boolean hasAdditionalPdf = artefact.getListType().hasAdditionalPdf()
                && artefact.getLanguage() != Language.ENGLISH;
            byte[] additionalPdf = hasAdditionalPdf ? getFileBytes(artefact, FileType.PDF, true)
                : new byte[0];
            byte[] excel = artefact.getListType().hasExcel() ? getFileBytes(artefact, FileType.EXCEL, false)
                : new byte[0];

            bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                                EmailHelper.maskEmail(subscriptionEmail.getEmail()))));
                rawDataSubscriptionEmailRequest(subscriptionEmail, artefact, artefactSummary, pdf, additionalPdf,
                                                excel, locationName);
            });
        }
    }

    private String flatFileSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact,
                                                    byte[] artefactFlatFile, String locationName) {
        FlatFileSubscriptionEmailData emailData = new FlatFileSubscriptionEmailData(
            body, artefact, locationName, artefactFlatFile, fileRetentionWeeks
        );
        EmailToSend email = emailService.handleEmailGeneration(emailData,
                                                               Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL);

        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    private String rawDataSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact,
                                                   String artefactSummary, byte[] pdf, byte[] additionalPdf,
                                                   byte[] excel, String locationName) {
        RawDataSubscriptionEmailData emailData = new RawDataSubscriptionEmailData(
            body, artefact, artefactSummary, pdf, additionalPdf, excel, locationName, fileRetentionWeeks
        );
        EmailToSend email = emailService.handleEmailGeneration(emailData,
                                                               Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL);

        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    private String getArtefactSummary(Artefact artefact) {
        if (payloadWithinLimit(artefact.getPayloadSize())) {
            return channelManagementService.getArtefactSummary(artefact.getArtefactId());
        }
        return "";
    }

    private byte[] getFileBytes(Artefact artefact, FileType fileType, boolean additionalPdf) {
        if (payloadWithinLimit(artefact.getPayloadSize())) {
            String artefactFile = channelManagementService.getArtefactFile(artefact.getArtefactId(),
                                                                           fileType, additionalPdf);
            return Base64.getDecoder().decode(artefactFile);
        }
        return new byte[0];
    }

    private boolean payloadWithinLimit(Float payloadSize) {
        return payloadSize == null || payloadSize < maxPayloadSize;
    }
}
