package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.FlatFileSubscriptionEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.RawDataSubscriptionEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.Base64;
import java.util.List;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class SubscriptionNotificationService {
    private final EmailService emailService;
    private final DataManagementService dataManagementService;
    private final ChannelManagementService channelManagementService;

    @Value("${payload.json.max-size-in-kb}")
    private int maxPayloadSize;

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
    public String subscriptionEmailRequest(SubscriptionEmail body) {
        log.info(writeLog(String.format("Sending subscription email for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        List<String> locations = body.getSubscriptions().get(SubscriptionTypes.LOCATION_ID);
        String locationName = CollectionUtils.isEmpty(locations) ? ""
            : dataManagementService.getLocation(locations.get(0)).getName();

        return artefact.getIsFlatFile().equals(Boolean.TRUE)
            ? flatFileSubscriptionEmailRequest(body, artefact, locationName)
            : rawDataSubscriptionEmailRequest(body, artefact, locationName);
    }

    private String flatFileSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact, String locationName) {
        byte[] artefactFlatFile = dataManagementService.getArtefactFlatFile(body.getArtefactId());
        FlatFileSubscriptionEmailBody emailBody = new FlatFileSubscriptionEmailBody(body, artefact, locationName,
                                                                                    artefactFlatFile);
        EmailToSend email = emailService.handleEmailGeneration(emailBody,
                                                               Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL);

        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }

    private String rawDataSubscriptionEmailRequest(SubscriptionEmail body, Artefact artefact, String locationName) {
        String artefactSummary = getArtefactSummary(artefact);
        byte[] pdf = getFileBytes(artefact, FileType.PDF, false);
        boolean hasAdditionalPdf = artefact.getListType().hasAdditionalPdf()
            && artefact.getLanguage() != Language.ENGLISH;
        byte[] additionalPdf = hasAdditionalPdf ? getFileBytes(artefact, FileType.PDF, true)
            : new byte[0];
        byte[] excel = artefact.getListType().hasExcel() ? getFileBytes(artefact, FileType.EXCEL, false)
            : new byte[0];

        RawDataSubscriptionEmailBody emailBody = new RawDataSubscriptionEmailBody(body, artefact, artefactSummary, pdf,
                                                                                  additionalPdf, excel, locationName);
        EmailToSend email = emailService.handleEmailGeneration(emailBody,
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
