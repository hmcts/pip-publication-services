package uk.gov.hmcts.reform.pip.publication.services.service;

import com.microsoft.applicationinsights.core.dependencies.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.FileType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

/**
 * This class handles any personalisation for the emails.
 */
@Component
@Slf4j
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.TooManyMethods"})
public class PersonalisationService {

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @Autowired
    ChannelManagementService channelManagementService;

    @Autowired
    FileCreationService fileCreationService;

    @Value("${env-name}")
    private String envName;

    @Value("${file-retention-weeks}")
    private String fileRetentionWeeks;

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
    public Map<String, Object> buildRawDataSubscriptionPersonalisation(SubscriptionEmail body,
                                                                       Artefact artefact) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();

            Map<SubscriptionTypes, List<String>> subscriptions = body.getSubscriptions();

            populateGenericPersonalisation(personalisation, DISPLAY_CASE_NUMBERS, CASE_NUMBERS,
                                           subscriptions.get(SubscriptionTypes.CASE_NUMBER)
            );

            populateGenericPersonalisation(personalisation, DISPLAY_CASE_URN, CASE_URN,
                                           subscriptions.get(SubscriptionTypes.CASE_URN)
            );

            populateLocationPersonalisation(personalisation, subscriptions.get(SubscriptionTypes.LOCATION_ID));

            personalisation.put("list_type", artefact.getListType());

            personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());

            Map<FileType, byte[]> publicationFiles =
                channelManagementService.getArtefactFiles(artefact.getArtefactId());

            byte[] artefactPdf = publicationFiles.get(FileType.PDF);
            byte[] artefactExcel = publicationFiles.get(FileType.EXCEL);

            boolean pdfWithinSize = artefactPdf.length < 2_000_000 && artefactPdf.length > 0;
            boolean excelWithinSize = artefactExcel.length < 2_000_000 && artefactExcel.length > 0;

            personalisation.put("display_pdf", pdfWithinSize);
            personalisation.put(LINK_TO_FILE, pdfWithinSize ? prepareUpload(artefactPdf, false,
                                false, fileRetentionWeeks) : "");

            personalisation.put("display_excel", excelWithinSize);
            personalisation.put("excel_link_to_file", excelWithinSize ? prepareUpload(artefactExcel,
                        false, false, fileRetentionWeeks) : "");

            personalisation.put("testing_of_array",
                                channelManagementService.getArtefactSummary(artefact.getArtefactId()));

            return personalisation;
        } catch (Exception e) {
            log.warn("Error adding attachment to raw data email {}. Artefact ID: {}",
                     EmailHelper.maskEmail(body.getEmail()),
                     artefact.getArtefactId()
            );
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
    public Map<String, Object> buildFlatFileSubscriptionPersonalisation(SubscriptionEmail body,
                                                                        Artefact artefact) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            List<String> location = body.getSubscriptions().get(SubscriptionTypes.LOCATION_ID);
            populateLocationPersonalisation(personalisation, location);

            personalisation.put("list_type", artefact.getListType());

            byte[] artefactData = dataManagementService.getArtefactFlatFile(body.getArtefactId());

            String sourceArtefactId = artefact.getSourceArtefactId();
            JSONObject uploadedFile = !Strings.isNullOrEmpty(sourceArtefactId) && sourceArtefactId.endsWith(".csv")
                ? prepareUpload(artefactData, true,
                                                   false, fileRetentionWeeks)
                : prepareUpload(artefactData, false, false, fileRetentionWeeks);

            personalisation.put(LINK_TO_FILE, uploadedFile);
            personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());

            return personalisation;
        } catch (NotificationClientException e) {

            log.warn("Error adding attachment to flat file email {}. Artefact ID: {}",
                     EmailHelper.maskEmail(body.getEmail()),
                     artefact.getArtefactId());
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
            personalisation.put(LINK_TO_FILE, prepareUpload(csvMediaApplications, true,
                                                                        false, fileRetentionWeeks));
            personalisation.put(ENV_NAME, convertEnvironmentName(envName));
            return personalisation;
        } catch (NotificationClientException e) {
            log.error(String.format("Error adding the csv attachment to the media application "
                                        + "reporting email with error %s", e.getMessage()));
            throw new NotifyException(e.getMessage());
        }
    }

    /**
     * Handles the personalisation for the unidentified blob email.
     *
     * @param locationMap A map of location Ids and provenances associated with unidentified blobs.
     * @return The personalisation map for the unidentified blob email.
     */
    public Map<String, Object> buildUnidentifiedBlobsPersonalisation(Map<String, String> locationMap) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        List<String> listOfUnmatched = new ArrayList<>();

        locationMap.forEach((key, value) ->
                                listOfUnmatched.add(String.format("%s - %s", key, value)));


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

    private void populateGenericPersonalisation(Map<String, Object> personalisation, String display,
                                                String displayValue, List<String> content) {
        if (content == null || content.isEmpty()) {
            personalisation.put(display, NO);
            personalisation.put(displayValue, "");
        } else {
            personalisation.put(display, YES);
            personalisation.put(displayValue, content);
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
            personalisation.put(LINK_TO_FILE, prepareUpload(excel, false,
                false, fileRetentionWeeks));
            personalisation.put(ENV_NAME, convertEnvironmentName(envName));
        } catch (IOException e) {
            log.warn("Error generating excel file attachment");
            throw new ExcelCreationException(e.getMessage());
        } catch (NotificationClientException e) {
            log.warn("Error adding attachment to MI data reporting email");
            throw new NotifyException(e.getMessage());
        }
        return personalisation;
    }
}
