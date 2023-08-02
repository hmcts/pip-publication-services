package uk.gov.hmcts.reform.pip.publication.services.service;

import org.jose4j.base64url.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationAction;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ExcelCreationException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.CaseNameHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
class PersonalisationServiceTest {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String ADMIN_DASHBOARD_LINK = "admin_dashboard_link";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String LINK_TO_FILE = "link_to_file";
    private static final String EXCEL_LINK_TO_FILE = "excel_link_to_file";
    private static final String FILE = "file";
    private static final String IS_CSV = "is_csv";
    private static final String FORENAME = "first_name";
    private static final String FULL_NAME = "FULL_NAME";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String LAST_SIGNED_IN_DATE = "11 July 2022";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String EMAIL = "a@b.com";
    private static final String CASE_URN_VALUE = "1234";
    private static final String CASE_NUMBER_VALUE = "12345678";
    private static final String LOCATION_ID = "12345";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final String ARRAY_OF_IDS = "array_of_ids";
    private static final String LINK_TO_FILE_MESSAGE = "Link to file does not match expected value";
    private static final String LIST_TYPE_MESSAGE = "List type does not match expected list type";
    private static final String LOCATION_MESSAGE = "Location not as expected";
    private static final String VERIFICATION_PAGE_LINK = "verification_page_link";
    private static final String CONTENTS = "Contents";
    private static final String ERROR_MESSAGE = "error message";
    private static final String REQUESTER_NAME = "requestor_name";
    private static final String CHANGE_TYPE = "change-type";
    private static final String ACTION_RESULT = "attempted/succeeded";
    private static final String ADDITIONAL_DETAILS = "Additional_change_detail";
    private static final String LOCATION_NAME = "location-name";
    private static final String CONTENT_DATE = "content_date";
    private static final String CONTENT_DATE_ASSERT_MESSAGE = "No content date found";
    private static final String REJECT_REASONS = "reject-reasons";
    private static final String FULL_NAME_LOWERCASE = "full-name";

    @Autowired
    PersonalisationService personalisationService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @MockBean
    DataManagementService dataManagementService;

    @MockBean
    ChannelManagementService channelManagementService;

    @MockBean
    CaseNameHelper caseNameHelper;

    @MockBean
    FileCreationService fileCreationService;

    private static Location location;
    private static final UUID ARTEFACT_ID = UUID.randomUUID();

    private static final String HELLO = "hello";
    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();

    private static final Map<SubscriptionTypes, List<String>> SUBSCRIPTIONS = new ConcurrentHashMap<>();
    private static final SubscriptionEmail SUBSCRIPTIONS_EMAIL = new SubscriptionEmail();

    private static final byte[] TEST_BYTE_ARRAY = HELLO.getBytes();
    private static final String BASE64_ENCODED_TEST_STRING = Base64.encode(TEST_BYTE_ARRAY);

    @BeforeAll
    public static void setup() {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(ARTEFACT_ID, "TEST", "1234"));

        location = new Location();
        location.setName("Location Name");

        SUBSCRIPTIONS.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        SUBSCRIPTIONS.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));
        SUBSCRIPTIONS.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SUBSCRIPTIONS_EMAIL.setEmail(EMAIL);
        SUBSCRIPTIONS_EMAIL.setArtefactId(ARTEFACT_ID);
        SUBSCRIPTIONS_EMAIL.setSubscriptions(SUBSCRIPTIONS);
    }

    @Test
    void testBuildMediaRejectionPersonalisation() throws IOException {
        Map<String, List<String>> reasons = new ConcurrentHashMap<>();

        reasons.put("notMedia", List.of(
            "The applicant is not an accredited member of the media.",
            "You can sign in with an existing MyHMCTS account. Or you can register your organisation at "
                + "https://www.gov.uk/guidance/myhmcts-online-case-management-for-legal-professionals"));

        reasons.put("noMatch", List.of(
            "Details provided do not match.",
            "The name, email address and Press ID do not match each other."));


        MediaRejectionEmail mediaRejectionEmail = new MediaRejectionEmail(
            "Test Name",
            "test_email@address.com",
            reasons
        );

        Map<String, Object> result = personalisationService.buildMediaRejectionPersonalisation(mediaRejectionEmail);

        assertNotNull(result, "Personalisation map should not be null");
        assertEquals(3, result.size(), "Personalisation map should contain 2 items");
        assertEquals("Test Name", result.get(FULL_NAME_LOWERCASE), "Full name should match the value set in "
            + "MediaRejectionEmail");
        assertEquals(
            List.of("The applicant is not an accredited member of the media.\n"
                + "^You can sign in with an existing MyHMCTS account. Or you can register your organisation at "
                + "https://www.gov.uk/guidance/myhmcts-online-case-management-for-legal-professionals",
                "Details provided do not match.\n"
                + "^The name, email address and Press ID do not match each other."),
            result.get(REJECT_REASONS),
            "Reasons should match the value set in MediaRejectionEmail"
        );
    }

    @Test
    void testBuildWelcomePersonalisation() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        WelcomeEmail welcomeEmail =
            new WelcomeEmail(EMAIL, false, FULL_NAME);

        Map<String, Object> personalisation = personalisationService.buildWelcomePersonalisation(welcomeEmail);

        Object subscriptionPageLink = personalisation.get(SUBSCRIPTION_PAGE_LINK);
        assertNotNull(subscriptionPageLink, "No subscription page link key found");
        assertEquals(personalisationLinks.getSubscriptionPageLink(), subscriptionPageLink,
                     "Subscription page link does not match expected link"
        );

        Object startPageLink = personalisation.get(START_PAGE_LINK);
        assertNotNull(startPageLink, "No start page link key found");
        assertEquals(personalisationLinks.getStartPageLink(), startPageLink,
                     "Start page link does not match expected link"
        );

        Object govGuidencePageLink = personalisation.get(GOV_GUIDANCE_PAGE_LINK);
        assertNotNull(govGuidencePageLink, "No gov guidance page link key found");
        assertEquals(personalisationLinks.getGovGuidancePageLink(), govGuidencePageLink,
                     "gov guidance page link does not match expected link"
        );
    }

    @Test
    void testBuildAdminAccountPersonalisation() {
        CreatedAdminWelcomeEmail createdAdminWelcomeEmail =
            new CreatedAdminWelcomeEmail(EMAIL, "firstname", "surname");

        Map<String, Object> personalisation =
            personalisationService.buildAdminAccountPersonalisation(createdAdminWelcomeEmail);

        Object forename = personalisation.get(FORENAME);
        assertNotNull(forename, "No forename key found");
        assertEquals(createdAdminWelcomeEmail.getForename(), forename,
                     "Forename does not match expected forename"
        );

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        Object aadResetLink = personalisation.get(AAD_RESET_LINK);
        assertNotNull(aadResetLink, "No aad reset link key found");
        assertEquals(personalisationLinks.getAadPwResetLinkAdmin(), aadResetLink,
                     "aad reset link does not match expected link"
        );

        Object aadSignInLink = personalisation.get(ADMIN_DASHBOARD_LINK);
        assertNotNull(aadSignInLink, "No admin dashboard link found");
        assertEquals(personalisationLinks.getAdminDashboardLink(), aadSignInLink,
                     "Admin dashboard link does not match expected link"
        );
    }

    @Test
    void buildRawDataWhenAllPresent() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setContentDate(LocalDateTime.now());
        artefact.setListType(ListType.SJP_PUBLIC_LIST);
        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(channelManagementService.getArtefactSummary(any())).thenReturn(HELLO);
        when(channelManagementService.getArtefactFile(any(), any())).thenReturn(BASE64_ENCODED_TEST_STRING);
        when(caseNameHelper.generateCaseNumberPersonalisation(any(), any())).thenReturn(
            SUBSCRIPTIONS.get(SubscriptionTypes.CASE_NUMBER));

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(YES, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals(SUBSCRIPTIONS.get(SubscriptionTypes.CASE_NUMBER), personalisation.get(CASE_NUMBERS),
                     "Case number not as expected"
        );
        assertEquals(YES, personalisation.get(DISPLAY_CASE_URN), "Display case urn is not Yes");
        assertEquals(SUBSCRIPTIONS.get(SubscriptionTypes.CASE_URN), personalisation.get(CASE_URN),
                     "Case urn not as expected"
        );
        assertEquals(YES, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not Yes");
        assertEquals(location.getName(), personalisation.get(LOCATIONS),
                     LOCATION_MESSAGE
        );
        assertEquals("SJP Public List", personalisation.get("list_type"),
                     LIST_TYPE_MESSAGE
        );
        assertEquals(Base64.encode(TEST_BYTE_ARRAY), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );
        assertEquals(Base64.encode(TEST_BYTE_ARRAY), ((JSONObject) personalisation.get(EXCEL_LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );
        assertEquals(HELLO, personalisation.get("testing_of_array"),
                     "testing_of_array does not match expected value"
        );

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        Object startPageLink = personalisation.get(START_PAGE_LINK);
        assertNotNull(startPageLink, "No start page link key found");
        assertEquals(personalisationLinks.getStartPageLink(), startPageLink,
                     "Start page link does not match expected link"
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void buildFlatFileWhenAllPresentAndNotCsv() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setContentDate(LocalDateTime.now());
        artefact.setSourceArtefactId("sourceArtefact.pdf");

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = CONTENTS.getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(YES, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not Yes");
        assertEquals(location.getName(), personalisation.get(LOCATIONS),
                     LOCATION_MESSAGE
        );
        assertEquals("Civil Daily Cause List", personalisation.get("list_type"),
                     LIST_TYPE_MESSAGE
        );
        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );
        assertEquals(false, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has been marked as a CSV when it's not"
        );

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        Object startPageLink = personalisation.get(START_PAGE_LINK);
        assertNotNull(startPageLink, "No start page link key found");
        assertEquals(personalisationLinks.getStartPageLink(), startPageLink,
                     "Start page link does not match expected link"
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void buildFlatFileWhenAllPresentAndCsv() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setContentDate(LocalDateTime.now());
        artefact.setSourceArtefactId("sourceArtefact.csv");

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = CONTENTS.getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );

        assertEquals(true, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has not been marked as a CSV when it is"
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void buildFlatFileWhenAllBlankSourceArtefactId() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setContentDate(LocalDateTime.now());

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = CONTENTS.getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );

        assertEquals(false, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has been marked as a CSV when it's not"
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void buildFlatFileWhenUploadCreationFailed() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] overSizeArray = new byte[2_100_000];
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(overSizeArray);

        assertThrows(NotifyException.class, () ->
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact));
    }

    @Test
    void testLocationMissing() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setContentDate(LocalDateTime.now());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(channelManagementService.getArtefactSummary(any())).thenReturn(HELLO);
        when(channelManagementService.getArtefactFile(any(), any())).thenReturn(BASE64_ENCODED_TEST_STRING);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not No");
        assertEquals("", personalisation.get(LOCATIONS),
                     LOCATION_MESSAGE
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void testNonLocationMissing() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        UUID uuid = UUID.randomUUID();
        subscriptionEmail.setArtefactId(uuid);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setContentDate(LocalDateTime.now());

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(channelManagementService.getArtefactSummary(any())).thenReturn(HELLO);
        when(channelManagementService.getArtefactFile(any(), any())).thenReturn(BASE64_ENCODED_TEST_STRING);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals("", personalisation.get(CASE_NUMBERS),
                     "Case number not as expected"
        );

        Object contentDate = personalisation.get(CONTENT_DATE);
        assertNotNull(contentDate, CONTENT_DATE_ASSERT_MESSAGE);
    }

    @Test
    void testBuildMediaApplicationReportingPersonalisation() {
        Map<String, Object> personalisation = personalisationService
            .buildMediaApplicationsReportingPersonalisation(TEST_BYTE);

        Object csvFile = personalisation.get(LINK_TO_FILE);
        assertNotNull(csvFile, "No csvFile key was found");
    }

    @Test
    void testBuildDuplicateMediaAccountPersonalisation() {
        DuplicatedMediaEmail duplicatedMediaEmail = new DuplicatedMediaEmail();
        duplicatedMediaEmail.setEmail(EMAIL);
        duplicatedMediaEmail.setFullName(FULL_NAME);

        Map<String, Object> personalisation = personalisationService
            .buildDuplicateMediaAccountPersonalisation(duplicatedMediaEmail);

        Object fullNameObject = personalisation.get("full_name");
        assertNotNull(fullNameObject, "No full name found");
        assertEquals(FULL_NAME, fullNameObject,
                     "Full name does not match"
        );

        Object mediaSignInPageLink = personalisation.get(AAD_SIGN_IN_LINK);
        assertNotNull(mediaSignInPageLink, "No media sign page link key found");
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        assertEquals(personalisationLinks.getAadSignInPageLink(), mediaSignInPageLink,
                     "Media Sign in page link does not match expected link"
        );
    }

    @Test
    void testBuildUnidentifiedBlobsPersonalisation() {
        Map<String, Object> personalisation = personalisationService
            .buildUnidentifiedBlobsPersonalisation(NO_MATCH_ARTEFACT_LIST);
        List<String> expectedData = new ArrayList<>();
        expectedData.add(String.format("1234 - TEST (%s)", ARTEFACT_ID));

        assertEquals(expectedData, personalisation.get(ARRAY_OF_IDS),
                     "Locations map not as expected"
        );
    }

    @Test
    void testBuildMediaVerificationPersonalisation() {
        MediaVerificationEmail mediaVerificationEmail = new MediaVerificationEmail(FULL_NAME, EMAIL);

        Map<String, Object> personalisation = personalisationService
            .buildMediaVerificationPersonalisation(mediaVerificationEmail);

        Object fullNameObject = personalisation.get("full_name");
        assertNotNull(fullNameObject, "No full name found");
        assertEquals(FULL_NAME, fullNameObject, "Full name does not match");

        Object mediaVerificationLink = personalisation.get(VERIFICATION_PAGE_LINK);
        assertNotNull(mediaVerificationLink, "No media verification link key found");
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        assertEquals(personalisationLinks.getMediaVerificationPageLink(), mediaVerificationLink,
                     "Media verification link does not match expected link"
        );
    }

    @Test
    void testBuildInactiveUserNotificationPersonalisation() {
        InactiveUserNotificationEmail inactiveUserNotificationEmail = new InactiveUserNotificationEmail(
            EMAIL, FULL_NAME, "PI_AAD", LAST_SIGNED_IN_DATE);

        assertThat(personalisationService
                       .buildInactiveUserNotificationPersonalisation(inactiveUserNotificationEmail))
            .as("Personalisation data does not match")
            .hasSize(4)
            .extracting(
                p -> p.get("full_name"),
                p -> p.get("last_signed_in_date"),
                p -> p.get("sign_in_page_link"),
                p -> p.get("cft_sign_in_link")
            )
            .containsExactly(
                FULL_NAME,
                LAST_SIGNED_IN_DATE,
                "https://pip-frontend.staging.platform.hmcts.net/admin-dashboard",
                "https://pip-frontend.staging.platform.hmcts.net/cft-login"
            );
    }

    @Test
    void testBuildMiDataReportingPersonalisation() throws IOException {
        when(fileCreationService.generateMiReport()).thenReturn(TEST_BYTE);
        assertThat(personalisationService.buildMiDataReportingPersonalisation())
            .as("Personalisation data does not match")
            .hasSize(2)
            .extracting(p -> ((JSONObject) p.get(LINK_TO_FILE)).get(FILE))
            .isEqualTo(Base64.encode(TEST_BYTE));
    }

    @Test
    void testBuildMiDataReportingWithExcelCreationException() throws IOException {
        when(fileCreationService.generateMiReport()).thenThrow(new IOException(ERROR_MESSAGE));
        assertThatThrownBy(() -> personalisationService.buildMiDataReportingPersonalisation())
            .isInstanceOf(ExcelCreationException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    void testBuildMiDataReportingWithNotifyException() throws IOException {
        when(fileCreationService.generateMiReport()).thenReturn(TEST_BYTE);
        try (MockedStatic<NotificationClient> mockStatic = mockStatic(NotificationClient.class)) {
            mockStatic.when(() -> EmailClient.prepareUpload(TEST_BYTE, false,
                                                            false, "78 weeks"
                ))
                .thenThrow(new NotificationClientException(ERROR_MESSAGE));
            assertThatThrownBy(() -> personalisationService.buildMiDataReportingPersonalisation())
                .isInstanceOf(NotifyException.class)
                .hasMessage(ERROR_MESSAGE);
        }
    }

    @Test
    void testBuildSystemAdminUpdateEmailPersonalisation() {

        DeleteLocationAction systemAdminAction = new DeleteLocationAction();
        systemAdminAction.setRequesterName(FULL_NAME);
        systemAdminAction.setEmailList(List.of(EMAIL));
        systemAdminAction.setChangeType(ChangeType.DELETE_LOCATION);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);
        systemAdminAction.setDetailString("Testing details");

        Map<String, Object> personalisation =
            personalisationService.buildSystemAdminUpdateEmailPersonalisation(systemAdminAction);

        Object requesterName = personalisation.get(REQUESTER_NAME);
        assertNotNull(requesterName, "No Requester name found");
        assertEquals(systemAdminAction.getRequesterName(), requesterName,
                     "Name does not match requester name"
        );

        Object changeType = personalisation.get(CHANGE_TYPE);
        assertNotNull(changeType, "No change type found");
        assertEquals(systemAdminAction.getChangeType().label, changeType,
                     "Change Type does not match"
        );

        Object actionResult = personalisation.get(ACTION_RESULT);
        assertNotNull(actionResult, "No action result found");
        assertEquals(systemAdminAction.getActionResult().label.toLowerCase(Locale.ENGLISH), actionResult,
                     "Action result does not match"
        );

        Object additionalDetails = personalisation.get(ADDITIONAL_DETAILS);
        assertNotNull(additionalDetails, "No additional information found");
        assertEquals(systemAdminAction.getDetailString(), additionalDetails,
                     "Additional information result does not match"
        );

    }

    @Test
    void testBuildDeleteLocationSubscriptionEmailPersonalisation() {

        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();
        locationSubscriptionDeletion.setLocationName(LOCATIONS);
        locationSubscriptionDeletion.setSubscriberEmails(List.of(EMAIL));

        Map<String, Object> personalisation =
            personalisationService.buildDeleteLocationSubscriptionEmailPersonalisation(locationSubscriptionDeletion);

        Object locationName = personalisation.get(LOCATION_NAME);
        assertNotNull(locationName, "No location name found");
        assertEquals(locationSubscriptionDeletion.getLocationName(), locationName,
                     "Name does not match location name"
        );
    }
}
