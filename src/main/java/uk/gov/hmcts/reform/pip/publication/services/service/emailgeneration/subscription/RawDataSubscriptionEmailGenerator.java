package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.CaseNameHelper;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.service.notify.NotificationClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
/**
 * Generate the raw data subscription email with personalisation for GOV.UK Notify template.
 */
public class RawDataSubscriptionEmailGenerator extends EmailGenerator {
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final int MAX_FILE_SIZE = 2_000_000;

    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        RawDataSubscriptionEmailData emailData = (RawDataSubscriptionEmailData) email;
        return generateEmail(emailData.getEmail(), MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, emailData.getArtefact(), personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(RawDataSubscriptionEmailData emailData, Artefact artefact,
                                                          PersonalisationLinks personalisationLinks) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            Map<SubscriptionTypes, List<String>> subscriptions = emailData.getSubscriptions();

            populateCaseNumberPersonalisation(artefact, personalisation,
                                              subscriptions.get(SubscriptionTypes.CASE_NUMBER));
            populateCaseUrnPersonalisation(personalisation, subscriptions.get(SubscriptionTypes.CASE_URN));
            populateLocationPersonalisation(personalisation, emailData.getLocationName());

            personalisation.put("list_type", artefact.getListType().getFriendlyName());
            personalisation.put("start_page_link", personalisationLinks.getStartPageLink());
            personalisation.put("subscription_page_link", personalisationLinks.getSubscriptionPageLink());
            personalisation.putAll(populateFilesPersonalisation(emailData, artefact));
            personalisation.putAll(populateSummaryPersonalisation(emailData.getArtefactSummary()));

            personalisation.put(
                "content_date",
                artefact.getContentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            );
            return personalisation;
        } catch (Exception e) {
            log.warn(writeLog(
                String.format("Error adding attachment to raw data email %s. Artefact ID: %s",
                              EmailHelper.maskEmail(emailData.getEmail()), emailData.getArtefact().getArtefactId())
            ));
            throw new NotifyException(e.getMessage());
        }
    }

    private void populateCaseNumberPersonalisation(Artefact artefact, Map<String, Object> personalisation,
                                                   List<String> content) {

        if (content == null || content.isEmpty()) {
            personalisation.put(DISPLAY_CASE_NUMBERS, NO);
            personalisation.put(CASE_NUMBERS, "");
        } else {
            personalisation.put(DISPLAY_CASE_NUMBERS, YES);
            personalisation.put(CASE_NUMBERS, CaseNameHelper.generateCaseNumberPersonalisation(artefact, content));
        }
    }

    private void populateCaseUrnPersonalisation(Map<String, Object> personalisation, List<String> content) {

        if (content == null || content.isEmpty()) {
            personalisation.put(DISPLAY_CASE_URN, NO);
            personalisation.put(CASE_URN, "");
        } else {
            personalisation.put(DISPLAY_CASE_URN, YES);
            personalisation.put(CASE_URN, content);
        }
    }

    private void populateLocationPersonalisation(Map<String, Object> personalisation, String locationName) {
        personalisation.put("display_locations", !locationName.isEmpty());
        personalisation.put("locations", locationName);
    }

    private Map<String, Object> populateFilesPersonalisation(RawDataSubscriptionEmailData emailData, Artefact artefact)
        throws NotificationClientException {
        Map<String, Object> personalisation = populatePdfPersonalisation(emailData, artefact);

        byte[] artefactExcelBytes = emailData.getExcel();
        boolean excelWithinSize = artefactExcelBytes.length < MAX_FILE_SIZE && artefactExcelBytes.length > 0;

        personalisation.put(
            "excel_link_text",
            excelWithinSize ? "Download the case list as an Excel spreadsheet." : ""
        );

        personalisation.put(
            "excel_link_to_file",
            excelWithinSize ? prepareUpload(artefactExcelBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        return personalisation;
    }

    private Map<String, Object> populatePdfPersonalisation(RawDataSubscriptionEmailData emailData, Artefact artefact)
        throws NotificationClientException {
        byte[] artefactPdfBytes = emailData.getPdf();
        boolean pdfWithinSize = artefactPdfBytes.length < MAX_FILE_SIZE && artefactPdfBytes.length > 0;

        boolean hasAdditionalPdf = artefact.getListType().hasAdditionalPdf()
            && artefact.getLanguage() != Language.ENGLISH;

        Map<String, Object> personalisation = populateAdditionalPdfPersonalisation(emailData, artefactPdfBytes,
                                                                                   pdfWithinSize, hasAdditionalPdf);

        personalisation.put(
            "pdf_link_text",
            !hasAdditionalPdf && pdfWithinSize ? "Download the case list as a PDF." : ""
        );

        personalisation.put(
            "pdf_link_to_file",
            !hasAdditionalPdf && pdfWithinSize
                ? prepareUpload(artefactPdfBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        return personalisation;
    }

    private Map<String, Object> populateAdditionalPdfPersonalisation(RawDataSubscriptionEmailData emailData,
                                                                     byte[] artefactPdfBytes, boolean pdfWithinSize,
                                                                     boolean hasAdditionalPdf)
        throws NotificationClientException {
        byte[] artefactWelshPdfBytes = emailData.getAdditionalPdf();
        boolean welshPdfWithinSize = artefactWelshPdfBytes.length < MAX_FILE_SIZE;

        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put(
            "english_pdf_link_text",
            hasAdditionalPdf && pdfWithinSize ? "Download the case list in English as a PDF." : ""
        );

        personalisation.put(
            "english_pdf_link_to_file",
            hasAdditionalPdf && pdfWithinSize
                ? prepareUpload(artefactPdfBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        personalisation.put(
            "welsh_pdf_link_text",
            hasAdditionalPdf && welshPdfWithinSize ? "Download the case list in Welsh as a PDF." : ""
        );

        personalisation.put(
            "welsh_pdf_link_to_file",
            hasAdditionalPdf && welshPdfWithinSize
                ? prepareUpload(artefactWelshPdfBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        return personalisation;
    }

    private Map<String, Object> populateSummaryPersonalisation(String artefactSummary) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("display_summary", !artefactSummary.isEmpty());
        personalisation.put("testing_of_array", artefactSummary);

        return personalisation;
    }
}
