package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyMethods", "PMD.ImmutableField"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class NotifyTest {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\"}";
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
        "{\"apiDestination\": \"https://localhost:4444\", \"artefactId\": \"1d7cfeb3-3e4d-44f8-a185-80b9a8971676\"}";
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
    private static final String NONEXISTENT_BLOB_SUBS_EMAIL = "{\n"
        + "  \"artefactId\": \"b190522a-5d9b-4089-a8c8-6918721c93df\",\n"
        + "  \"email\": \"daniel.furnivall1@justice.gov.uk\",\n"
        + "  \"subscriptions\": {\n"
        + "    \"CASE_URN\": [\n"
        + "      \"123\"\n"
        + "    ]\n"
        + "  }\n"
        + "}";

    private static final String VALID_SUBS_EMAIL = "{\n"
        + "  \"artefactId\": \"c8327f76-19e0-4190-84a7-49eeac89fd21\",\n"
        + "  \"email\": \"daniel.furnivall1@justice.gov.uk\",\n"
        + "  \"subscriptions\": {\n"
        + "    \"CASE_URN\": [\n"
        + "      \"123\"\n"
        + "    ]\n"
        + "  }\n"
        + "}";

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
            .andExpect(status().isOk()).andExpect(content().string(containsString(
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

    @Test
    void testValidPayloadForSubsEmailReturnsOk() throws Exception {
        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(VALID_SUBS_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Subscription email successfully sent to")));
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
}
