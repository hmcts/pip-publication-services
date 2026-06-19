package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.CaseInfoHelper;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.subscription.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.service.notify.NotificationClientException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_EXCEL_EMAIL_V2;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_NO_DOWNLOAD_LINK_EMAIL_V2;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_PDF_EMAIL_V2;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_PDF_EXCEL_EMAIL_V2;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

/**
 * Generate the raw data subscription email with personalisation for GOV.UK Notify template.
 */
@Service
@Slf4j
// NOSONAR - TODO SonarQube complains about duplicate code, ignore this as the old version is going to be removed in the future
public class RawDataSubscriptionEmailGeneratorV2 extends EmailGenerator {
    private static final String CASE = "case";
    private static final String DISPLAY_CASE = "display_case";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final int MAX_FILE_SIZE = 2_000_000;

    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        RawDataSubscriptionEmailData emailData = (RawDataSubscriptionEmailData) email;
        Map<String, Object> personalisations = buildEmailPersonalisation(
            emailData, emailData.getArtefact(), personalisationLinks
        );
        Templates template = determineTemplate(personalisations);
        return generateEmail(emailData, template.getTemplate(), personalisations);
    }

    private Templates determineTemplate(Map<String, Object> personalisations) {
        boolean hasPdf = personalisations.containsKey("pdf_link_to_file")
            && !personalisations.get("pdf_link_to_file").toString().isEmpty();
        boolean hasExcel = personalisations.containsKey("excel_link_to_file")
            && !personalisations.get("excel_link_to_file").toString().isEmpty();

        if (hasPdf) {
            if (hasExcel) {
                return MEDIA_SUBSCRIPTION_PDF_EXCEL_EMAIL_V2;
            } else {
                return MEDIA_SUBSCRIPTION_PDF_EMAIL_V2;
            }
        } else if (hasExcel) {
            return MEDIA_SUBSCRIPTION_EXCEL_EMAIL_V2;
        } else {
            return MEDIA_SUBSCRIPTION_NO_DOWNLOAD_LINK_EMAIL_V2;
        }
    }

    private Map<String, Object> buildEmailPersonalisation(RawDataSubscriptionEmailData emailData, Artefact artefact,
                                                          PersonalisationLinks personalisationLinks) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            Map<SubscriptionTypes, List<String>> subscriptions = emailData.getSubscriptions();

            populateCasePersonalisation(artefact, personalisation, subscriptions);
            populateLocationPersonalisation(personalisation, emailData.getLocationName());

            personalisation.put("list_type", artefact.getListType().getFriendlyName());
            personalisation.put("start_page_link", personalisationLinks.getStartPageLink());
            personalisation.put("subscription_page_link", personalisationLinks.getSubscriptionPageLink());
            personalisation.putAll(populateFilesPersonalisation(emailData));
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

    private void populateCasePersonalisation(Artefact artefact, Map<String, Object> personalisation,
                                             Map<SubscriptionTypes, List<String>> subscriptions) {
        List<String> caseNumberSubscriptionContent = subscriptions.get(SubscriptionTypes.CASE_NUMBER);
        List<String> caseNameSubscriptionContent = subscriptions.get(SubscriptionTypes.CASE_NAME);

        if (CollectionUtils.isEmpty(caseNumberSubscriptionContent)
            && CollectionUtils.isEmpty(caseNameSubscriptionContent)) {
            personalisation.put(DISPLAY_CASE, NO);
            personalisation.put(CASE, "");
        } else {
            personalisation.put(DISPLAY_CASE, YES);
            List<String> cases = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(caseNumberSubscriptionContent)) {
                cases.addAll(
                    CaseInfoHelper.generateCaseNumberPersonalisationV2(artefact, caseNumberSubscriptionContent)
                );
            }
            if (CollectionUtils.isNotEmpty(caseNameSubscriptionContent)) {
                cases.addAll(
                    CaseInfoHelper.generateCaseNamePersonalisationV2(artefact, caseNameSubscriptionContent)
                );
            }
            List<String> deduplicatedCases = cases.stream()
                .distinct()
                .collect(Collectors.toList());

            personalisation.put(CASE, deduplicatedCases);
        }
    }

    private void populateLocationPersonalisation(Map<String, Object> personalisation, String locationName) {
        personalisation.put("display_locations", !locationName.isEmpty());
        personalisation.put("locations", locationName);
    }

    private Map<String, Object> populateFilesPersonalisation(RawDataSubscriptionEmailData emailData)
        throws NotificationClientException {
        Map<String, Object> personalisation = populatePdfPersonalisation(emailData);
        personalisation.putAll(populateExcelPersonalisation(emailData));
        return personalisation;
    }

    private Map<String, Object> populatePdfPersonalisation(RawDataSubscriptionEmailData emailData)
        throws NotificationClientException {
        byte[] artefactPdfBytes = emailData.getPdf();
        boolean pdfWithinSize = artefactPdfBytes.length < MAX_FILE_SIZE && artefactPdfBytes.length > 0;

        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put(
            "pdf_link_text", pdfWithinSize ? "Download the case list as a PDF." : ""
        );
        personalisation.put(
            "pdf_link_to_file", pdfWithinSize
                ? prepareUpload(artefactPdfBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        return personalisation;
    }

    private Map<String, Object> populateExcelPersonalisation(RawDataSubscriptionEmailData emailData)
        throws NotificationClientException {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        byte[] excelBytes = emailData.getExcel();
        boolean excelWithinSize = excelBytes.length < MAX_FILE_SIZE && excelBytes.length > 0;

        personalisation.put(
            "excel_link_text",
            excelWithinSize ? "Download the case list as an Excel spreadsheet." : ""
        );

        personalisation.put(
            "excel_link_to_file",
            excelWithinSize ? prepareUpload(excelBytes, false, emailData.getFileRetentionWeeks()) : ""
        );

        return personalisation;
    }

    private Map<String, Object> populateSummaryPersonalisation(String artefactSummary) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("display_summary", !artefactSummary.isEmpty());
        personalisation.put("summary_of_cases", artefactSummary);

        return personalisation;
    }
}
