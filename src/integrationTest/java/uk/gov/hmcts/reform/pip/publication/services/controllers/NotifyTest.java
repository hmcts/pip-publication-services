package uk.gov.hmcts.reform.pip.publication.services.controllers;

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

import java.io.IOException;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyMethods", "PMD.ImmutableField"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
class NotifyTest {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\"}";
    private static final String VALID_WELCOME_REQUEST_BODY_NEW =
        "{\"email\": \"test@email.com\", \"isExisting\": \"false\"}";
    private static final String VALID_ADMIN_CREATION_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"surname\": \"surname\", \"forename\": \"forename\"}";
    private static final String INVALID_JSON_BODY = "{\"email\": \"test@email.com\", \"isExisting\":}";
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";
    private static final String THIRD_PARTY_SUBSCRIPTION_JSON_BODY =
        "{\"apiDestination\": \"https://localhost:4444\", \"artefactIds\": [\"1d7cfeb3-3e4d-44f8-a185-80b9a8971676\"]}";
    private static final String THIRD_PARTY_SUBSCRIPTION_FILE_BODY =
        "{\"apiDestination\": \"https://localhost:4444\", \"artefactIds\": [\"79f5c9ae-a951-44b5-8856-3ad6b7454b0e\"]}";
    private static final String THIRD_PARTY_SUBSCRIPTION_INVALID_ARTEFACT_BODY =
        "{\"apiDestination\": \"http://localhost:4444\", \"artefactIds\": [\"1e565487-23e4-4a25-9364-43277a5180d4\"]}";
    private static final String API_SUBSCRIPTION_URL = "/notify/api";
    private static final String EXTERNAL_PAYLOAD = "test";
    private static final String SUBSCRIPTION_URL = "/notify/subscription";
    private static final String THIRD_PARTY_FAIL_MESSAGE = "Third party request to: https://localhost:4444 "
        + "failed after 3 retries due to: 404 Not Found from POST https://localhost:4444";

    private MockWebServer externalApiMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);
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
    @WithMockUser(username = "unknown_user", authorities = { "APPROLE_api.request.unknown" })
    void testUnauthorizedRequestWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "unknown_user", authorities = { "APPROLE_api.request.unknown" })
    void testUnauthorizedRequestAdminEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testNotifyApiSubscribersJson() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type",
                                                     ContentType.APPLICATION_JSON)
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
                                          .addHeader("Content-Type",
                                                     ContentType.APPLICATION_JSON)
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
    void testMissingEmailForSubscriptionReturnsBadRequest() throws Exception {

        String missingEmailJsonBody =
            "{\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}, \"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(missingEmailJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
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
}
