package uk.gov.hmcts.reform.pip.publication.services.service;

import org.jose4j.base64url.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PersonalisationServiceTest {

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String SURNAME = "surname";
    private static final String FORENAME = "first_name";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String YES = "Yes";
    private static final String NO = "No";

    @Autowired
    PersonalisationService personalisationService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    @MockBean
    DataManagementService dataManagementService;

    private static Location location;
    private UUID artefactId = UUID.randomUUID();

    @BeforeAll
    private static void setup() {
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
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();

        CreatedAdminWelcomeEmail createdAdminWelcomeEmail =
            new CreatedAdminWelcomeEmail("a@b.com", "firstname", "surname");

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
        String locationId = "12345";
        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of("1234"));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(locationId));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(locationId)).thenReturn(location);

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
        String locationId = "12345";
        byte[] fileContents = "Contents".getBytes();

        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of("1234"));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(locationId));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(locationId)).thenReturn(location);
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
        String locationId = "12345";
        byte[] overSizeArray = new byte[2100000];

        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of("1234"));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(locationId));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(artefactId);
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(artefactId);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(locationId)).thenReturn(location);
        when(dataManagementService.getArtefactFlatFile(artefactId)).thenReturn(overSizeArray);

        assertThrows(NotifyException.class, () ->
            personalisationService.buildFlatFileSubscriptionPersonalisation(subscriptionEmail, artefact));
    }

    @Test
    void testLocationMissing() {
        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        subscriptions.put(SubscriptionTypes.CASE_NUMBER, List.of("1234"));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
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
        String locationId = "12345";
        Map<SubscriptionTypes, List<String>> subscriptions = new HashMap<>();
        subscriptions.put(SubscriptionTypes.CASE_URN, List.of("1234"));
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of(locationId));

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail("a@b.com");
        subscriptionEmail.setArtefactId(UUID.randomUUID());
        subscriptionEmail.setSubscriptions(subscriptions);

        Artefact artefact = new Artefact();
        artefact.setArtefactId(UUID.randomUUID());
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        when(dataManagementService.getLocation(locationId)).thenReturn(location);

        Map<String, Object> personalisation =
            personalisationService.buildRawDataSubscriptionPersonalisation(subscriptionEmail, artefact);

        assertEquals(NO, personalisation.get(DISPLAY_CASE_NUMBERS), "Display case numbers is not Yes");
        assertEquals("", personalisation.get(CASE_NUMBERS),
                     "Case number not as expected");
    }




}
