package uk.gov.hmcts.reform.pip.publication.services.service;

import org.jose4j.base64url.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary.ArtefactSummaryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods"})
class PersonalisationServiceTest {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String LINK_TO_FILE = "link_to_file";
    private static final String FILE = "file";
    private static final String IS_CSV = "is_csv";
    private static final String SURNAME = "surname";
    private static final String FORENAME = "first_name";
    private static final String FULL_NAME = "FULL_NAME";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String EMAIL = "a@b.com";
    private static final String CASE_URN_VALUE = "1234";
    private static final String CASE_NUMBER_VALUE = "12345678";
    private static final String LOCATION_ID = "12345";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final String ARRAY_OF_IDS = "array_of_ids";
    private static final String LINK_TO_FILE_MESSAGE = "Link to file does not match expected value";

    @Autowired
    PersonalisationService personalisationService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @MockBean
    DataManagementService dataManagementService;

    @MockBean
    PdfCreationService pdfCreationService;

    @MockBean
    ArtefactSummaryService artefactSummaryService;

    private static Location location;
    private static final UUID ARTEFACT_ID = UUID.randomUUID();

    private static final String HELLO = "hello";
    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();

    private static final Map<SubscriptionTypes, List<String>> SUBSCRIPTIONS = new ConcurrentHashMap<>();
    private static final SubscriptionEmail SUBSCRIPTIONS_EMAIL = new SubscriptionEmail();

    @BeforeAll
    public static void setup() {
        LOCATIONS_MAP.put("test", "1234");

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
    void testBuildWelcomePersonalisation() {
        WelcomeEmail welcomeEmail =
            new WelcomeEmail(EMAIL, false, FULL_NAME);

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
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

        Object surname = personalisation.get(SURNAME);
        assertNotNull(surname, "No surname key found");
        assertEquals(createdAdminWelcomeEmail.getSurname(), surname,
                     "Surname does not match expected surname"
        );

        Object forename = personalisation.get(FORENAME);
        assertNotNull(forename, "No forename key found");
        assertEquals(createdAdminWelcomeEmail.getForename(), forename,
                     "Forename does not match expected forename"
        );

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        Object aadResetLink = personalisation.get(AAD_RESET_LINK);
        assertNotNull(aadResetLink, "No aad reset link key found");
        assertEquals(personalisationLinks.getAadPwResetLink(), aadResetLink,
                     "aad reset link does not match expected link"
        );

        Object aadSignInLink = personalisation.get(AAD_SIGN_IN_LINK);
        assertNotNull(aadSignInLink, "No aad sign in link key found");
        assertEquals(personalisationLinks.getAadSignInPageLink(), aadSignInLink,
                     "Aad Sign In link does not match expected link"
        );
    }

    @Test
    void buildRawDataWhenAllPresent() throws IOException {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        byte[] testByteArray = HELLO.getBytes();
        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("<Placeholder>");
        when(pdfCreationService.jsonToHtml(artefact.getArtefactId())).thenReturn(HELLO);
        when(pdfCreationService.generatePdfFromHtml(any())).thenReturn(testByteArray);
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("hi");

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
                     "Location not as expected"
        );
        assertEquals(ListType.CIVIL_DAILY_CAUSE_LIST, personalisation.get("list_type"),
                     "List type does not match expected list type"
        );
        assertEquals(Base64.encode(testByteArray), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );
        assertEquals("hi", personalisation.get("testing_of_array"),
                     "testing_of_array does not match expected value"
        );
    }

    @Test
    void buildRawDataFileWhenUploadFails() throws IOException {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        byte[] overSizeArray = new byte[2_100_000];
        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(pdfCreationService.jsonToHtml(artefact.getArtefactId())).thenReturn(HELLO);
        when(pdfCreationService.generatePdfFromHtml(HELLO)).thenReturn(overSizeArray);

        assertThrows(NotifyException.class, () ->
            personalisationService.buildRawDataSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact), "desired "
                         + "exception was not thrown");
    }

    @Test
    void buildFlatFileWhenAllPresentAndNotCsv() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setSourceArtefactId("sourceArtefact.pdf");

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = "Contents".getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(YES, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not Yes");
        assertEquals(location.getName(), personalisation.get(LOCATIONS),
                     "Location not as expected"
        );
        assertEquals(ListType.CIVIL_DAILY_CAUSE_LIST, personalisation.get("list_type"),
                     "List type does not match expected list type"
        );
        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );
        assertEquals(false, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has been marked as a CSV when it's not"
        );
    }

    @Test
    void buildFlatFileWhenAllPresentAndCsv() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setSourceArtefactId("sourceArtefact.csv");

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = "Contents".getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );

        assertEquals(true, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has not been marked as a CSV when it is"
        );
    }

    @Test
    void buildFlatFileWhenAllBlankSourceArtefactId() {


        Artefact artefact = new Artefact();
        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = "Contents".getBytes();
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(SUBSCRIPTIONS_EMAIL, artefact);

        assertEquals(Base64.encode(fileContents), ((JSONObject) personalisation.get(LINK_TO_FILE)).get(FILE),
                     LINK_TO_FILE_MESSAGE
        );

        assertEquals(false, ((JSONObject) personalisation.get(LINK_TO_FILE)).get(IS_CSV),
                     "File has been marked as a CSV when it's not"
        );
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
    void testLocationMissing() throws IOException {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("hi");
        when(pdfCreationService.jsonToHtml(artefact.getArtefactId())).thenReturn(HELLO);
        when(pdfCreationService.generatePdfFromHtml(HELLO)).thenReturn(HELLO.getBytes());
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("hi");

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not No");
        assertEquals("", personalisation.get(LOCATIONS),
                     "Location not as expected"
        );
    }

    @Test
    void testNonLocationMissing() throws IOException {
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

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn("h");
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("hi");
        when(pdfCreationService.jsonToHtml(artefact.getArtefactId())).thenReturn(HELLO);
        when(pdfCreationService.generatePdfFromHtml(HELLO)).thenReturn(HELLO.getBytes());
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn("h");
        when(artefactSummaryService.artefactSummary(any(), any())).thenReturn("hi");

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals("", personalisation.get(CASE_NUMBERS),
                     "Case number not as expected"
        );
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
        assertEquals(fullNameObject, FULL_NAME,
                     "Full name does not match");

        Object mediaSignInPageLink = personalisation.get(AAD_SIGN_IN_LINK);
        assertNotNull(mediaSignInPageLink, "No media sign page link key found");
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        assertEquals(personalisationLinks.getAadSignInPageLink(), mediaSignInPageLink,
                     "Media Sign in page link does not match expected link");
    }

    @Test
    void testBuildUnidentifiedBlobsPersonalisation() {
        Map<String, Object> personalisation = personalisationService
            .buildUnidentifiedBlobsPersonalisation(LOCATIONS_MAP);
        List<String> expectedData = new ArrayList<>();
        expectedData.add("test - 1234");

        assertEquals(expectedData, personalisation.get(ARRAY_OF_IDS),
                     "Locations map not as expected");
    }
}
