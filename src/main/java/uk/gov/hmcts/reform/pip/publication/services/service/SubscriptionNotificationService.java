package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
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

    @Async
    public void flatFileBulkSubscriptionEmailRequest(BulkSubscriptionEmail bulkSubscriptionEmail,
                                                     Artefact artefact,
                                                     String locationName) {

        byte[] flatFileData = dataManagementService.getArtefactFlatFile(artefact.getArtefactId());
        bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {
            try {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                            EmailHelper.maskEmail(subscriptionEmail.getEmail()))));

                flatFileSubscriptionEmailRequest(subscriptionEmail, artefact, flatFileData, locationName);
            } catch (TooManyEmailsException ex) {
                log.error(writeLog(ex.getMessage()));
            } catch (NotifyException ignored) {
                // This is a bulk email, so we don't want to stop the process if one email fails
                // This exception is already logged at a higher level, so no need to log again here
            }
        });
    }

    @Async
    public void rawDataBulkSubscriptionEmailRequest(BulkSubscriptionEmail bulkSubscriptionEmail,
                                                    Artefact artefact, String locationName) {
        String artefactSummary = getArtefactSummary(artefact);
        byte[] pdf = getFileBytes(artefact, FileType.PDF, false);
        boolean hasAdditionalPdf = artefact.getListType().hasAdditionalPdf()
            && artefact.getLanguage() != Language.ENGLISH;
        byte[] additionalPdf = hasAdditionalPdf ? getFileBytes(artefact, FileType.PDF, true)
            : new byte[0];
        byte[] excel = artefact.getListType().hasExcel() ? getFileBytes(artefact, FileType.EXCEL, false)
            : new byte[0];

        bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {

            try {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                                EmailHelper.maskEmail(subscriptionEmail.getEmail()))));
                rawDataSubscriptionEmailRequest(subscriptionEmail, artefact, artefactSummary, pdf, additionalPdf,
                                                    excel, locationName);
            } catch (TooManyEmailsException ex) {
                log.error(writeLog(ex.getMessage()));
            } catch (NotifyException ignored) {
                // This is a bulk email, so we don't want to stop the process if one email fails
                // This exception is already logged at a higher level, so no need to log again here
            }
        });
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
