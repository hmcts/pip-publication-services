package uk.gov.hmcts.reform.pip.publication.services.service;

import com.microsoft.applicationinsights.core.dependencies.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.CaseNameHelper;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClientException;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.SJP_DELTA_PRESS_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.SJP_PRESS_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.SJP_PUBLIC_LIST;
import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

/**
 * This class handles any personalisation for the emails.
 */
@Component
@Slf4j
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class PersonalisationService {

    private static final int MAX_FILE_SIZE = 2_000_000;
    private static final String LINK_TO_SERVICE = "link-to-service";

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @Autowired
    ChannelManagementService channelManagementService;

    @Autowired
    FileCreationService fileCreationService;

    @Autowired
    CaseNameHelper caseNameHelper;

    @Value("${env-name}")
    private String envName;

    @Value("${file-retention-weeks}")
    private String fileRetentionWeeks;

    private static final String REJECT_REASONS = "reject-reasons";
    private static final String FULL_NAME_LOWERCASE = "full-name";
    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String ADMIN_DASHBOARD_LINK = "admin_dashboard_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String FORGOT_PASSWORD_PROCESS_LINK = "forgot_password_process_link";
    private static final String LINK_TO_FILE = "link_to_file";
    private static final String LAST_SIGNED_IN_DATE = "last_signed_in_date";
    private static final String FORENAME = "first_name";
    private static final String FULL_NAME = "full_name";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String ARRAY_OF_IDS = "array_of_ids";
    private static final String VERIFICATION_PAGE_LINK = "verification_page_link";
    private static final String CFT_SIGN_IN_LINK = "cft_sign_in_link";
    private static final String ENV_NAME = "env_name";
    private static final String REQUESTER_NAME = "requestor_name";
    private static final String CHANGE_TYPE = "change-type";
    private static final String ACTION_RESULT = "attempted/succeeded";
    private static final String ADDITIONAL_DETAILS = "Additional_change_detail";
    private static final String LOCATION_NAME = "location-name";


    /**
     * Handles the personalisation for the Welcome email.
     *
     * @return The personalisation map for the welcome email.
     */
    public Map<String, Object> buildWelcomePersonalisation(WelcomeEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(FORGOT_PASSWORD_PROCESS_LINK, notifyConfigProperties.getLinks().getAadPwResetLinkMedia());
        personalisation.put(SUBSCRIPTION_PAGE_LINK, notifyConfigProperties.getLinks().getSubscriptionPageLink());
        personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());
        personalisation.put(GOV_GUIDANCE_PAGE_LINK, notifyConfigProperties.getLinks().getGovGuidancePageLink());
        personalisation.put(FULL_NAME, body.getFullName());
        return personalisation;
    }

    /**
     * Handles the personalisation for the admin account email.
     *
     * @param body The body of the admin email.
     * @return The personalisation map for the admin account email.
     */
    public Map<String, Object> buildAdminAccountPersonalisation(CreatedAdminWelcomeEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(FORENAME, body.getForename());
        personalisation.put(AAD_RESET_LINK, notifyConfigProperties.getLinks().getAadPwResetLinkAdmin());
        personalisation.put(ADMIN_DASHBOARD_LINK, notifyConfigProperties.getLinks().getAdminDashboardLink());
        return personalisation;
    }

    /**
     * Handles the personalisation for the raw data subscription email.
     *
     * @param body     The body of the subscription.
     * @param artefact The artefact to send in the subscription.
     * @return The personalisation map for the raw data subscription email.
     */
    public Map<String, Object> buildRawDataSubscriptionPersonalisation(SubscriptionEmail body, Artefact artefact) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();

            Map<SubscriptionTypes, List<String>> subscriptions = body.getSubscriptions();

            populateCaseNumberPersonalisation(
                artefact,
                personalisation,
                subscriptions.get(SubscriptionTypes.CASE_NUMBER)
            );

            populateCaseUrnPersonalisation(personalisation, subscriptions.get(SubscriptionTypes.CASE_URN));

            populateLocationPersonalisation(personalisation, subscriptions.get(SubscriptionTypes.LOCATION_ID));

            personalisation.put("list_type", artefact.getListType().getFriendlyName());

            personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());

            String artefactPdf = channelManagementService.getArtefactFile(artefact.getArtefactId(), FileType.PDF);
            byte[] artefactPdfBytes = Base64.getDecoder().decode(artefactPdf);
            byte[] artefactExcelBytes = new byte[0];

            if (SJP_PUBLIC_LIST.equals(artefact.getListType())
                || SJP_DELTA_PRESS_LIST.equals(artefact.getListType())
                || SJP_PRESS_LIST.equals(artefact.getListType())) {
                String artefactExcel = channelManagementService.getArtefactFile(artefact.getArtefactId(),
                                                                                FileType.EXCEL);
                artefactExcelBytes = Base64.getDecoder().decode(artefactExcel);
            }

            boolean pdfWithinSize = artefactPdfBytes.length < MAX_FILE_SIZE && artefactPdfBytes.length > 0;
            boolean excelWithinSize = artefactExcelBytes.length < MAX_FILE_SIZE && artefactExcelBytes.length > 0;

            personalisation.put("display_pdf", pdfWithinSize);
            personalisation.put(
                LINK_TO_FILE,
                pdfWithinSize ? prepareUpload(artefactPdfBytes, false, false, fileRetentionWeeks) : ""
            );

            personalisation.put("display_excel", excelWithinSize);
            personalisation.put(
                "excel_link_to_file",
                excelWithinSize ? prepareUpload(artefactExcelBytes, false, false, fileRetentionWeeks) : ""
            );

            personalisation.put(
                "testing_of_array",
                channelManagementService.getArtefactSummary(artefact.getArtefactId())
            );

            personalisation.put(
                "content_date",
                artefact.getContentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            );

            return personalisation;
        } catch (Exception e) {
            log.warn(writeLog(
                String.format("Error adding attachment to raw data email %s. Artefact ID: %s",
                              EmailHelper.maskEmail(body.getEmail()), artefact.getArtefactId())
            ));
            throw new NotifyException(e.getMessage());
        }
    }

    /**
     * Handles the personalisation for the flat file subscription email.
     *
     * @param body     The body of the subscription.
     * @param artefact The artefact to send in the subscription.
     * @return The personalisation map for the flat file subscription email.
     */
    public Map<String, Object> buildFlatFileSubscriptionPersonalisation(SubscriptionEmail body, Artefact artefact) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            List<String> location = body.getSubscriptions().get(SubscriptionTypes.LOCATION_ID);
            populateLocationPersonalisation(personalisation, location);

            personalisation.put("list_type", artefact.getListType().getFriendlyName());

            byte[] artefactData = dataManagementService.getArtefactFlatFile(body.getArtefactId());

            String sourceArtefactId = artefact.getSourceArtefactId();
            JSONObject uploadedFile = !Strings.isNullOrEmpty(sourceArtefactId) && sourceArtefactId.endsWith(".csv")
                ? prepareUpload(artefactData, true, false, fileRetentionWeeks) :
                prepareUpload(artefactData, false, false, fileRetentionWeeks);

            personalisation.put(LINK_TO_FILE, uploadedFile);
            personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());

            personalisation.put(
                "content_date",
                artefact.getContentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            );

            return personalisation;
        } catch (NotificationClientException e) {
            log.warn(writeLog(String.format(
                "Error adding attachment to flat file email {}. Artefact ID: {}",
                EmailHelper.maskEmail(body.getEmail()),
                artefact.getArtefactId()
            )));
            throw new NotifyException(e.getMessage());
        }
    }

    /**
     * Handles the personalisation for the media reporting email.
     *
     * @param csvMediaApplications The csv byte array containing the media applications.
     * @return The personalisation map for the media reporting email.
     */
    public Map<String, Object> buildMediaApplicationsReportingPersonalisation(byte[] csvMediaApplications) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            personalisation.put(LINK_TO_FILE, prepareUpload(csvMediaApplications, true, false, fileRetentionWeeks));
            personalisation.put(ENV_NAME, convertEnvironmentName(envName));
            return personalisation;
        } catch (NotificationClientException e) {
            log.error(writeLog(String.format(
                "Error adding the csv attachment to the media application " + "reporting email with error %s",
                e.getMessage()
            )));
            throw new NotifyException(e.getMessage());
        }
    }

    /**
     * Handles the personalisation for the unidentified blob email.
     *
     * @param noMatchArtefactList A list of no match artefacts for the unidentified blob email.
     * @return The personalisation map for the unidentified blob email.
     */
    public Map<String, Object> buildUnidentifiedBlobsPersonalisation(List<NoMatchArtefact> noMatchArtefactList) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        List<String> listOfUnmatched = new ArrayList<>();

        noMatchArtefactList.forEach(noMatchArtefact -> listOfUnmatched.add(String.format(
            "%s - %s (%s)",
            noMatchArtefact.getLocationId(),
            noMatchArtefact.getProvenance(),
            noMatchArtefact.getArtefactId()
        )));

        personalisation.put(ARRAY_OF_IDS, listOfUnmatched);
        personalisation.put(ENV_NAME, convertEnvironmentName(envName));
        return personalisation;
    }

    /**
     * Handles the personalisation for the media verification email.
     *
     * @param body The body of the media verification email.
     * @return The personalisation map for the media verification email.
     */
    public Map<String, Object> buildMediaVerificationPersonalisation(MediaVerificationEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(FULL_NAME, body.getFullName());
        personalisation.put(VERIFICATION_PAGE_LINK, notifyConfigProperties.getLinks().getMediaVerificationPageLink());
        return personalisation;
    }

    /**
     * Handles the personalisation for the media account rejection email.
     *
     * @param body The body of the media account rejection email.
     * @return The personalisation map for the media rejection email.
     */
    public Map<String, Object> buildMediaRejectionPersonalisation(MediaRejectionEmail body) throws IOException {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();

        personalisation.put(FULL_NAME_LOWERCASE, body.getFullName());

        personalisation.put(REJECT_REASONS, formatReasons(body.getReasons()));
        personalisation.put(LINK_TO_SERVICE, notifyConfigProperties.getLinks().getStartPageLink()
            + "/create-media-account");

        return personalisation;
    }

    private static List<String> formatReasons(Map<String, List<String>> reasons) {
        List<String> reasonList = new ArrayList<>();
        reasons.forEach((key, value) -> reasonList.add(String.format("%s\n^%s", value.get(0), value.get(1))));
        return reasonList;
    }

    /**
     * Handles the personalisation for the inactive user notification email.
     *
     * @param body The body of the inactive user notification email.
     * @return The personalisation map for the inactive user notification email.
     */
    public Map<String, Object> buildInactiveUserNotificationPersonalisation(InactiveUserNotificationEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(FULL_NAME, body.getFullName());
        personalisation.put(LAST_SIGNED_IN_DATE, body.getLastSignedInDate());
        personalisation.put(AAD_SIGN_IN_LINK, notifyConfigProperties.getLinks().getAadAdminSignInPageLink());
        personalisation.put(CFT_SIGN_IN_LINK, notifyConfigProperties.getLinks().getCftSignInPageLink());
        return personalisation;
    }

    public Map<String, Object> buildSystemAdminUpdateEmailPersonalisation(SystemAdminAction body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(REQUESTER_NAME, body.getRequesterName());
        personalisation.put(ACTION_RESULT, body.getActionResult().label.toLowerCase(Locale.ENGLISH));
        personalisation.put(CHANGE_TYPE, body.getChangeType().label);
        personalisation.put(ADDITIONAL_DETAILS, body.createAdditionalChangeDetail());
        personalisation.put(ENV_NAME, convertEnvironmentName(envName));

        return personalisation;
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

    private void populateCaseNumberPersonalisation(Artefact artefact, Map<String, Object> personalisation,
                                                   List<String> content) {

        if (content == null || content.isEmpty()) {
            personalisation.put(DISPLAY_CASE_NUMBERS, NO);
            personalisation.put(CASE_NUMBERS, "");
        } else {
            personalisation.put(DISPLAY_CASE_NUMBERS, YES);
            personalisation.put(CASE_NUMBERS, caseNameHelper.generateCaseNumberPersonalisation(artefact, content));
        }
    }

    private void populateLocationPersonalisation(Map<String, Object> personalisation, List<String> content) {
        if (content == null || content.isEmpty()) {
            personalisation.put(DISPLAY_LOCATIONS, NO);
            personalisation.put(LOCATIONS, "");
        } else {
            Location subLocation = dataManagementService.getLocation(content.get(0));
            personalisation.put(DISPLAY_LOCATIONS, YES);
            personalisation.put(LOCATIONS, subLocation.getName());
        }
    }

    /**
     * Handles the personalisation for the duplicate media account email.
     *
     * @param body The body of the admin email.
     * @return The personalisation map for the duplicate media account email.
     */
    public Map<String, Object> buildDuplicateMediaAccountPersonalisation(DuplicatedMediaEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(FULL_NAME, body.getFullName());
        personalisation.put(AAD_SIGN_IN_LINK, notifyConfigProperties.getLinks().getAadSignInPageLink());
        return personalisation;
    }

    /**
     * Handles the personalisation for the MI data reporting email.
     *
     * @return The personalisation map for the email.
     */
    public Map<String, Object> buildMiDataReportingPersonalisation() {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        try {
            byte[] excel = fileCreationService.generateMiReport();
            personalisation.put(LINK_TO_FILE, prepareUpload(excel, false, false, fileRetentionWeeks));
            personalisation.put(ENV_NAME, convertEnvironmentName(envName));
        } catch (IOException e) {
            log.warn(writeLog("Error generating excel file attachment"));
            throw new ExcelCreationException(e.getMessage());
        } catch (NotificationClientException e) {
            log.warn(writeLog("Error adding attachment to MI data reporting email"));
            throw new NotifyException(e.getMessage());
        }
        return personalisation;
    }

    public Map<String, Object> buildDeleteLocationSubscriptionEmailPersonalisation(LocationSubscriptionDeletion body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(LOCATION_NAME, body.getLocationName());

        return personalisation;
    }
}
