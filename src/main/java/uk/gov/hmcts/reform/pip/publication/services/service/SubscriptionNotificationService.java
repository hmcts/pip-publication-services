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

    @Value("${payload.json.max-size-summary}")
    private int maxPayloadSizeForSummary;

    @Value("${file-retention-weeks}")
    private int fileRetentionWeeks;

    @Autowired
    public SubscriptionNotificationService(EmailService emailService, DataManagementService dataManagementService) {
        this.emailService = emailService;
        this.dataManagementService = dataManagementService;
    }

    private String flatFileSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact, byte[] artefactFlatFile,
                                                    String locationName, String referenceId) {
        FlatFileSubscriptionEmailData emailData = new FlatFileSubscriptionEmailData(
            body, artefact, locationName, artefactFlatFile, fileRetentionWeeks, referenceId
        );
        EmailToSend email = emailService.handleEmailGeneration(
            emailData, Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL
        );

        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    private String rawDataSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact, String artefactSummary,
                                                   byte[] pdf, byte[] excel, String locationName,
                                                   String referenceId) {
        RawDataSubscriptionEmailData emailData = new RawDataSubscriptionEmailData(
            body, artefact, artefactSummary, pdf, excel, locationName, fileRetentionWeeks, referenceId
        );
        EmailToSend email = emailService.handleEmailGeneration(emailData, Templates.MEDIA_SUBSCRIPTION_PDF_EXCEL_EMAIL);

        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    @Async
    public void flatFileBulkSubscriptionEmailRequest(BulkSubscriptionEmail bulkSubscriptionEmail, Artefact artefact,
                                                     String locationName, String referenceId) {

        byte[] flatFileData = dataManagementService.getArtefactFlatFile(artefact.getArtefactId());
        bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {
            try {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                            EmailHelper.maskEmail(subscriptionEmail.getEmail()))));

                flatFileSubscriptionEmailRequest(subscriptionEmail, artefact, flatFileData, locationName, referenceId);
            } catch (TooManyEmailsException ex) {
                log.error(writeLog(ex.getMessage()));
            } catch (NotifyException ignored) {
                // This is a bulk email, so we don't want to stop the process if one email fails
                // This exception is already logged at a higher level, so no need to log again here
            }
        });
    }

    @Async
    public void rawDataBulkSubscriptionEmailRequest(BulkSubscriptionEmail bulkSubscriptionEmail, Artefact artefact,
                                                    String locationName, String referenceId) {
        String artefactSummary = getArtefactSummary(artefact);
        byte[] pdf;

        if (artefact.getListType().hasAdditionalPdf()
            && artefact.getLanguage().equals(Language.WELSH)) {
            pdf = getFileBytes(artefact, FileType.PDF, true);
        } else {
            pdf = getFileBytes(artefact, FileType.PDF, false);
        }

        byte[] excel = artefact.getListType().hasExcel() ? getFileBytes(artefact, FileType.EXCEL, false)
            : new byte[0];

        bulkSubscriptionEmail.getSubscriptionEmails().forEach(subscriptionEmail -> {

            try {
                log.info(writeLog(String.format("Sending subscription email for user %s",
                                                EmailHelper.maskEmail(subscriptionEmail.getEmail()))));
                rawDataSubscriptionEmailRequest(subscriptionEmail, artefact, artefactSummary,  pdf,
                                                excel, locationName, referenceId);
            } catch (TooManyEmailsException ex) {
                log.error(writeLog(ex.getMessage()));
            } catch (NotifyException ignored) {
                // This is a bulk email, so we don't want to stop the process if one email fails
                // This exception is already logged at a higher level, so no need to log again here
            }
        });
    }

    private String getArtefactSummary(Artefact artefact) {
        if (payloadWithinLimitForSummary(artefact.getPayloadSize())) {
            return dataManagementService.getArtefactSummary(artefact.getArtefactId());
        }
        return "";
    }

    private byte[] getFileBytes(Artefact artefact, FileType fileType, boolean additionalPdf) {
        String artefactFile = dataManagementService.getArtefactFile(artefact.getArtefactId(),
                                                                    fileType, additionalPdf);
        return Base64.getDecoder().decode(artefactFile);
    }

    private boolean payloadWithinLimitForSummary(Float payloadSize) {
        return payloadSize == null || payloadSize < maxPayloadSizeForSummary;
    }

}
