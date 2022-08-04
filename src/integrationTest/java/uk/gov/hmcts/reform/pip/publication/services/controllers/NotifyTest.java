package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyMethods",
    "PMD.ImmutableField", "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("functional")
class NotifyTest {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\", \"fullName\": \"fullName\"}";
    private static final String VALID_WELCOME_REQUEST_BODY_NEW =
        "{\"email\": \"test@email.com\", \"isExisting\": \"false\", \"fullName\": \"fullName\"}";
    private static final String VALID_ADMIN_CREATION_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"surname\": \"surname\", \"forename\": \"forename\"}";
    private static final String INVALID_JSON_BODY = "{\"email\": \"test@email.com\", \"isExisting\":}";
    private static final String VALID_DUPLICATE_MEDIA_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"fullName\": \"fullName\"}";
    private static final String DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY =
        "{\"email\": \"test@email.com\", \"fullName\":}";
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";
    private static final String MEDIA_REPORTING_EMAIL_URL = "/notify/media/report";
    private static final String THIRD_PARTY_SUBSCRIPTION_JSON_BODY =
        "{\"apiDestination\": \"https://localhost:4444\", \"artefactId\": \"70494df0-31c1-4290-bbd2-7bfe7acfeb81\"}";
    private static final String THIRD_PARTY_SUBSCRIPTION_FILE_BODY =
        "{\"apiDestination\": \"https://localhost:4444\", \"artefactId\": \"79f5c9ae-a951-44b5-8856-3ad6b7454b0e\"}";
    private static final String THIRD_PARTY_SUBSCRIPTION_INVALID_ARTEFACT_BODY =
        "{\"apiDestination\": \"http://localhost:4444\", \"artefactId\": \"1e565487-23e4-4a25-9364-43277a5180d4\"}";
    private static final String API_SUBSCRIPTION_URL = "/notify/api";
    private static final String EXTERNAL_PAYLOAD = "test";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    private static final String VALID_API_DESTINATION = "https://localhost:4444";
    public static final String SUBS_EMAIL_SUCCESS = "Subscription email successfully sent to";

    private static final String NEW_LINE_WITH_BRACKET = "{\n";
    private static final String SUBSCRIPTION_REQUEST = "\"subscriptions\": {\n\n"
        + "    \"CASE_URN\": [\n\n"
        + "      \"123\"\n\n"
        + "    ]\n\n"
        + "  }\n\n"
        + "}\"";
    private static final String EMAIL_SEND_MESSAGE = "Subscription email successfully sent to";

    private static final String NONEXISTENT_BLOB_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"b190522a-5d9b-4089-a8c8-6918721c93df\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_CIVIL_CAUSE_LIST_SUBS_EMAIL = "{\n"
        + "  \"artefactId\": \"82c33285-ab4b-4c8e-8a80-b9ea7dc67db8\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + "  \"subscriptions\": {\n"
        + "    \"LOCATION_ID\": [\n"
        + "      \"2\"\n"
        + "    ]\n"
        + "  }\n"
        + "}";

    private static final String VALID_FAMILY_CAUSE_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"55b9e27b-d315-4c7e-9116-0b83939c03eb\",\n"
        + "  \"email\": \"junaid.iqbal@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_CIVIL_AND_FAMILY_CAUSE_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"af77ae82-b0c2-4515-8bc0-dc3fed1853d8\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_SJP_PUBLIC_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"e61a7e34-f950-4a6c-9200-7b94745b5a7a\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_SJP_PRESS_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"8cd9b0ad-0c5a-4220-9305-137d2d4862ef\",\n"
        + "  \"email\": \"daniel.furnivall1@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_COP_CAUSE_SUBS_EMAIL = "{\n"
        + "  \"artefactId\": \"887d58b1-c177-4564-b6b2-da47bf899747\",\n"
        + "  \"email\": \"test_account_verified@hmcts.net\",\n"
        + "  \"subscriptions\": {\n"
        + "    \"CASE_URN\": [\n"
        + "      \"123\"\n"
        + "    ]\n"
        + "  }\n"
        + "}";

    private static final String VALID_SCSS_DAILY_LIST_SUBS_EMAIL =
        "{\n  \"artefactId\": \"69745ab9-137b-4fd2-a15a-42cc85bf8d49\",\n"
            + "  \"email\": \"daniel.furnivall1@justice.gov.uk\",\n"
            + "  \"subscriptions\": {\n"
            + "    \"CASE_URN\": [\n"
            + "      \"123\"\n]\n}\n}";

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME
        ));

    String validMediaReportingJson;
    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();

    String validLocationsMapJson;

    private static final String SUBSCRIPTION_URL = "/notify/subscription";
    private static final String DUPLICATE_MEDIA_EMAIL_URL = "/notify/duplicate/media";
    private static final String THIRD_PARTY_FAIL_MESSAGE = "Third party request to: https://localhost:4444 "
        + "failed after 3 retries due to: 404 Not Found from POST https://localhost:4444";


    private static final Map<String, String> LIST_MAP = Map.of("SSCS Daily List",
                                                               VALID_SCSS_DAILY_LIST_SUBS_EMAIL,
                                                               "SJP Public List",
                                                               VALID_SJP_PUBLIC_SUBS_EMAIL,
                                                               "SJP Press List",
                                                               VALID_SJP_PRESS_SUBS_EMAIL,
                                                               "COP Daily List",
                                                               VALID_COP_CAUSE_SUBS_EMAIL,
                                                               "Family Daily Cause List",
                                                               VALID_FAMILY_CAUSE_LIST_SUBS_EMAIL,
                                                               "Civil and Family Daily Cause List",
                                                               VALID_CIVIL_AND_FAMILY_CAUSE_LIST_SUBS_EMAIL,
                                                               "Civil Daily Cause List",
                                                               VALID_CIVIL_CAUSE_LIST_SUBS_EMAIL
    );

    private MockWebServer externalApiMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        LOCATIONS_MAP.put("test", "1234");
        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

        validMediaReportingJson = ow.writeValueAsString(MEDIA_APPLICATION_LIST);
        validLocationsMapJson = ow.writeValueAsString(LOCATIONS_MAP);
    }

    @AfterEach
    void tearDown() throws IOException {
        externalApiMockServer.close();
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testValidPayloadReturnsSuccessNew() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadReturnsSuccessDuplicateMedia() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Duplicate media account email successfully sent")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequestDuplicateMedia() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadReturnsSuccessAdminCreation() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Created admin welcome email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequestAdminCreation() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "unknown_user", authorities = {"APPROLE_api.request.unknown"})
    void testUnauthorizedRequestWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "unknown_user", authorities = {"APPROLE_api.request.unknown"})
    void testUnauthorizedRequestAdminEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testNotifyApiSubscribersJson() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(
                                              "Content-Type",
                                              ContentType.APPLICATION_JSON
                                          )
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));
    }

    @Test
    void testNotifyApiSubscribersFile() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(
                                              "Content-Type",
                                              ContentType.APPLICATION_JSON
                                          )
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_FILE_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));

        // Assert request body sent to third party api
        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody)
            .isNotNull()
            .isNotEmpty()
            .contains("\"publicationDate\": \"2022-04-12T09:30:52.123Z\"");
        assertThat(isValidJson(requestBody)).isTrue();
    }

    @Test
    void testNotifyApiSubscribersThrowsBadGateway() throws Exception {
        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_INVALID_ARTEFACT_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadGateway());
    }

    @Test
    void testNotifyApiSubscriberReturnsError() throws Exception {
        externalApiMockServer.enqueue(new MockResponse().setResponseCode(404));
        externalApiMockServer.enqueue(new MockResponse().setResponseCode(404));
        externalApiMockServer.enqueue(new MockResponse().setResponseCode(404));
        externalApiMockServer.enqueue(new MockResponse().setResponseCode(404));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_FILE_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound()).andExpect(content().string(containsString(
                THIRD_PARTY_FAIL_MESSAGE)));
    }

    @Test
    void testValidPayloadMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testInvalidPayloadMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingEmailForSubscriptionReturnsBadRequest() throws Exception {

        String missingEmailJsonBody =
            "{\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}, \"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(missingEmailJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }


    @Test
    void testValidPayloadForSubsEmailThrowsBadGateway() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(NONEXISTENT_BLOB_SUBS_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadGateway());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SSCS Daily List", "SJP Public List", "SJP Press List", "COP Daily List"})
    void testValidPayloadForAllSubsEmailTypesReturnsOk(String listType) throws Exception {
        MvcResult value = mockMvc.perform(post(SUBSCRIPTION_URL)
                                              .content(LIST_MAP.get(listType))
                                              .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andReturn();
        assertThat(value.getResponse().getContentAsString()).as("Failed - List type = " + listType)
            .contains(SUBS_EMAIL_SUCCESS);
    }

    @Test
    void testInvalidEmailForSubscriptionReturnsBadRequest() throws Exception {

        String invalidEmailJsonBody =
            "{\"email\":\"abcd\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]},"
                + "\"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(invalidEmailJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadForSubsCivilDailyCauseListEmailReturnsOk() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(VALID_CIVIL_CAUSE_LIST_SUBS_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Subscription email successfully sent to")));
    }

    @Test
    void testValidPayloadForSubsFamilyCauseListListEmailReturnsOk() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(VALID_FAMILY_CAUSE_LIST_SUBS_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content().string(containsString(EMAIL_SEND_MESSAGE)));
    }

    @Test
    void testValidPayloadForSubsCivilAndFamilyCauseListEmailReturnsOk() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(VALID_CIVIL_AND_FAMILY_CAUSE_LIST_SUBS_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content().string(containsString(EMAIL_SEND_MESSAGE)));
    }

    @Test
    void testMissingArtefactIdForSubscriptionReturnsBadRequest() throws Exception {

        String missingArtefactIdJsonBody =
            "{\"email\":\"a@b.com\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(missingArtefactIdJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidSubscriptionCriteriaForSubscriptionReturnsBadRequest() throws Exception {

        String invalidSubscriptionJsonBody =
            "{\"email\":\"a@b.com\",\"subscriptions\": {\"LOCATION_ID\":[]},"
                + "\"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(invalidSubscriptionJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidFlatFileRequest() throws Exception {
        String validBody =
            "{\"email\":\"a@b.com\",\"subscriptions\": {\"LOCATION_ID\":[\"9\"]},"
                + "\"artefactId\": \"79f5c9ae-a951-44b5-8856-3ad6b7454b0e\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(validBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testDeletePayloadThirdParty() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(
                                              "Content-Type",
                                              ContentType.APPLICATION_JSON
                                          )
                                          .setResponseCode(200));

        mockMvc.perform(put(API_SUBSCRIPTION_URL)
                            .content(VALID_API_DESTINATION)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content()
                           .string(containsString("Successfully sent empty list to https://localhost:4444")));
    }

    @Test
    void testValidPayloadUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsMapJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andExpect(content().string(
                containsString("Unidentified blob email successfully sent with reference id:")));
    }

    @Test
    void testInvalidPayloadUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    private boolean isValidJson(String jsonString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
