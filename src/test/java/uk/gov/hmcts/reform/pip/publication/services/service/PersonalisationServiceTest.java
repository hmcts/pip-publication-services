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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PersonalisationServiceTest {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String LINK_TO_FILE = "link_to_file";
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

    @Autowired
    PersonalisationService personalisationService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @MockBean
    DataManagementService dataManagementService;

    private static Location location;
    private final UUID artefactId = UUID.randomUUID();

    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();

    @BeforeAll
    public static void setup() {
        LOCATIONS_MAP.put("test", "1234");

        location = new Location();
        location.setName("Location Name");
    }

    @Test
    void testBuildWelcomePersonalisation() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
        Map<String, Object> personalisation = personalisationService.buildWelcomePersonalisation();

        Object subscriptionPageLink = personalisation.get(SUBSCRIPTION_PAGE_LINK);
        assertNotNull(subscriptionPageLink, "No subscription page link key found");
        assertEquals(personalisationLinks.getSubscriptionPageLink(), subscriptionPageLink,
                     "Subscription page link does not match expected link");

        Object startPageLink = personalisation.get(START_PAGE_LINK);
        assertNotNull(startPageLink, "No start page link key found");
        assertEquals(personalisationLinks.getStartPageLink(), startPageLink,
                     "Start page link does not match expected link");

        Object govGuidencePageLink = personalisation.get(GOV_GUIDANCE_PAGE_LINK);
        assertNotNull(govGuidencePageLink, "No gov guidance page link key found");
        assertEquals(personalisationLinks.getGovGuidancePageLink(), govGuidencePageLink,
                     "gov guidance page link does not match expected link");
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
                     "Surname does not match expected surname");

        Object forename = personalisation.get(FORENAME);
        assertNotNull(forename, "No forename key found");
        assertEquals(createdAdminWelcomeEmail.getForename(), forename,
                     "Forename does not match expected forename");

        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        Object aadResetLink = personalisation.get(AAD_RESET_LINK);
        assertNotNull(aadResetLink, "No aad reset link key found");
        assertEquals(personalisationLinks.getAadPwResetLink(), aadResetLink,
                     "aad reset link does not match expected link");

        Object aadSignInLink = personalisation.get(AAD_SIGN_IN_LINK);
        assertNotNull(aadSignInLink, "No aad sign in link key found");
        assertEquals(personalisationLinks.getAadSignInPageLink(), aadSignInLink,
                     "Aad Sign In link does not match expected link");
    }

    @Test
    void buildRawDataWhenAllPresent() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(YES, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals(subscriptions.get(SubscriptionTypes.CASE_NUMBER), personalisation.get(CASE_NUMBERS),
                     "Case number not as expected");
        assertEquals(YES, personalisation.get(DISPLAY_CASE_URN), "Display case urn is not Yes");
        assertEquals(subscriptions.get(SubscriptionTypes.CASE_URN), personalisation.get(CASE_URN),
                     "Case urn not as expected");
        assertEquals(YES, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not Yes");
        assertEquals(location.getName(), personalisation.get(LOCATIONS),
                     "Location not as expected");
        assertEquals(ListType.CIVIL_DAILY_CAUSE_LIST, personalisation.get("list_type"),
                     "List type does not match expected list type");
        assertEquals("<Placeholder>", personalisation.get("link_to_file"),
                     "Link to file does not match expected value");
        assertEquals("<Placeholder>", personalisation.get("testing_of_array"),
                     "testing_of_array does not match expected value");
    }

    @Test
    void buildFlatFileWhenAllPresent() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] fileContents = "Contents".getBytes();
        when(dataManagementService.getArtefactFlatFile(artefactId)).thenReturn(fileContents);

        Map<String, Object> personalisation =
            personalisationService.buildFlatFileSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(YES, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not Yes");
        assertEquals(location.getName(), personalisation.get(LOCATIONS),
                     "Location not as expected");
        assertEquals(ListType.CIVIL_DAILY_CAUSE_LIST, personalisation.get("list_type"),
                     "List type does not match expected list type");
        assertEquals(Base64.encode(fileContents), ((JSONObject)personalisation.get("link_to_file")).get("file"),
                     "Link to file does not match expected value");
    }

    @Test
    void buildFlatFileWhenUploadCreationFailed() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN_VALUE));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of(CASE_NUMBER_VALUE));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        byte[] overSizeArray = new byte[2_100_000];
        when(dataManagementService.getArtefactFlatFile(artefactId)).thenReturn(overSizeArray);

        assertThrows(NotifyException.class, () ->
            personalisationService.buildFlatFileSubscriptionPersonalisation(subscriptionEmail, artefact));
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
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_LOCATIONS), "Display case locations is not No");
        assertEquals("", personalisation.get(LOCATIONS),
                     "Location not as expected");
    }

    @Test
    void testNonLocationMissing() {
        Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of(CASE_URN));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals("", personalisation.get(CASE_NUMBERS),
                     "Case number not as expected");
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
