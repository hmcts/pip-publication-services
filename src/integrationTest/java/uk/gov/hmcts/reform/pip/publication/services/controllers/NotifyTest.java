package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.client.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyMethods"})
@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
    private static final String MEDIA_REPORTING_EMAIL_URL = "/notify/media/report";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";

    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME));

    private static final Map<String, String> LOCATIONS_MAP = new ConcurrentHashMap<>();



    String validMediaReportingJson;
    String validLocationsMapJson;
    private static final String SUBSCRIPTION_URL = "/notify/subscription";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws JsonProcessingException {
        LOCATIONS_MAP.put("test", "1234");

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

        validMediaReportingJson = ow.writeValueAsString(MEDIA_APPLICATION_LIST);
        validLocationsMapJson = ow.writeValueAsString(LOCATIONS_MAP);
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
        try (MockWebServer mockPublicationServicesEndpoint = new MockWebServer()) {

            mockPublicationServicesEndpoint.start(8081);

            mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader(
                                                            "Content-Type",
                                                            ContentType.APPLICATION_JSON
                                                        )
                                                        .setBody("{\"artefactId\": \"" + UUID.randomUUID()
                                                                     + "\", \"isFlatFile\": true, \"listType\":"
                                                                     + "\"CIVIL_DAILY_CAUSE_LIST\"}")
                                                        .setResponseCode(200));

            mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader(
                                                            "Content-Type",
                                                            ContentType.APPLICATION_JSON
                                                        )
                                                        .setBody("{\"name\": \"thisIsAName\"}")
                                                        .setResponseCode(200));

            mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader(
                                                            "Content-Type",
                                                            ContentType.APPLICATION_OCTET_STREAM
                                                        )
                                                        .setBody("abcd")
                                                        .setResponseCode(200));

            String validBody =
                "{\"email\":\"a@b.com\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]},"
                    + "\"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

            mockMvc.perform(post(SUBSCRIPTION_URL)
                                .content(validBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }
    }

    @Test
    void testValidPayloadUnidentifiedBlobEmail() throws Exception {

        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsMapJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testInvalidPayloadUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
