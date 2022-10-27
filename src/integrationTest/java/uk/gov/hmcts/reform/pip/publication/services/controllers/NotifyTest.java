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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

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
    private static final String THIRD_PARTY_SUBSCRIPTION_ARTEFACT_BODY = "{\n"
        + "  \"apiDestination\": \"https://localhost:4444\",\n"
        + "  \"artefact\": {\n"
        + "    \"artefactId\": \"70494df0-31c1-4290-bbd2-7bfe7acfeb81\",\n"
        + "    \"listType\": \"CIVIL_DAILY_CAUSE_LIST\",\n"
        + "    \"locationId\": \"2\",\n"
        + "    \"provenance\": \"MANUAL_UPLOAD\",\n"
        + "    \"type\": \"LIST\",\n"
        + "    \"contentDate\": \"2022-06-09T07:36:35\",\n"
        + "    \"sensitivity\": \"PUBLIC\",\n"
        + "    \"language\": \"ENGLISH\",\n"
        + "    \"displayFrom\": \"2022-02-16T07:36:35\",\n"
        + "    \"displayTo\": \"2099-06-02T07:36:35\"\n"
        + "  }\n"
        + "}";

    private static final String API_SUBSCRIPTION_URL = "/notify/api";
    private static final String EXTERNAL_PAYLOAD = "test";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = "/notify/media/verification";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = "/notify/user/sign-in";
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    public static final String SUBS_EMAIL_SUCCESS = "Subscription email successfully sent to";

    private static final String NEW_LINE_WITH_BRACKET = "{\n";
    private static final String SUBSCRIPTION_REQUEST = "\"subscriptions\": {\n\n"
        + "    \"CASE_URN\": [\n\n"
        + "      \"123\"\n\n"
        + "    ]\n\n"
        + "  }\n\n"
        + "}\"";

    private static final String NONEXISTENT_BLOB_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"b190522a-5d9b-4089-a8c8-6918721c93df\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_CIVIL_CAUSE_LIST_SUBS_EMAIL = "{\n"
        + "  \"artefactId\": \"4d1e88f5-8457-4d93-8d11-1744a4bc16bd\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + "  \"subscriptions\": {\n"
        + "    \"LOCATION_ID\": [\n"
        + "      \"2\"\n"
        + "    ]\n"
        + "  }\n"
        + "}";

    private static final String VALID_FAMILY_CAUSE_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"f94f0d2d-27e6-46f1-8528-e33eeac5728d\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_CIVIL_AND_FAMILY_CAUSE_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"af77ae82-b0c2-4515-8bc0-dc3fed1853d8\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_SJP_PUBLIC_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"31889528-ad90-4535-a02d-b7dcc9de1102\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_SJP_PRESS_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"email\": \"test_account_verified@hmcts.net\",\n"
        + "  \"artefactId\": \"41ab3903-c87c-42b5-994f-9b55f6dcad48\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
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
        "{\n  \"artefactId\": \"468ff616-449a-4fed-bc77-62f6640c2067\",\n"
            + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
            + "  \"subscriptions\": {\n"
            + "    \"CASE_URN\": [\n"
            + "      \"123\"\n]\n}\n}";

    private static final String VALID_PRIMARY_HEALTH_TRIBUNAL_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"43bf6e71-9666-4a1d-98c2-fe2283395d49\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

    private static final String VALID_MEDIA_VERIFICATION_EMAIL_BODY =
        "{\"fullName\": \"fullName\", \"email\": \"test@email.com\"}";

    private static final String VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY =
        "{\"email\": \"test@test.com\", \"fullName\": \"testName\", \"lastSignedInDate\": \"01 May 2022\"}";

    private static final String VALID_CROWN_DAILY_LIST_SUBS_EMAIL = NEW_LINE_WITH_BRACKET
        + "  \"artefactId\": \"98292daa-4ca3-4fbe-8878-11878a056d57\",\n"
        + "  \"email\": \"test_account_admin@justice.gov.uk\",\n"
        + SUBSCRIPTION_REQUEST;

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
        assertThat(recordedRequest.getHeader("Content-Type"))
            .as("Incorrect content type in request header")
            .contains(MediaType.MULTIPART_FORM_DATA_VALUE);

        assertThat(recordedRequest.getBody().readUtf8())
            .as("Expected data missing in request body")
            .isNotNull()
            .isNotEmpty()
            .contains("\"publicationDate\": \"2022-04-12T09:30:52.123Z\"");
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
    @MethodSource("parameters")
    void testValidPayloadForAllSubsEmailTypesReturnsOk(String listType, String listSubscription) throws Exception {
        MvcResult value = mockMvc.perform(post(SUBSCRIPTION_URL)
                                              .content(listSubscription)
                                              .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andReturn();
        assertThat(value.getResponse().getContentAsString()).as("Failed - List type = " + listType)
            .contains(SUBS_EMAIL_SUCCESS);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of("SSCS Daily List", VALID_SCSS_DAILY_LIST_SUBS_EMAIL),
            Arguments.of("SJP Public List", VALID_SJP_PUBLIC_SUBS_EMAIL),
            Arguments.of("SJP Press List", VALID_SJP_PRESS_SUBS_EMAIL),
            Arguments.of("COP Daily List", VALID_COP_CAUSE_SUBS_EMAIL),
            Arguments.of("Family Daily Cause List", VALID_FAMILY_CAUSE_LIST_SUBS_EMAIL),
            Arguments.of("Civil and Family Daily Cause List", VALID_CIVIL_AND_FAMILY_CAUSE_LIST_SUBS_EMAIL),
            Arguments.of("Civil Daily Cause List", VALID_CIVIL_CAUSE_LIST_SUBS_EMAIL),
            Arguments.of("Primary Health Tribunal Hearing List", VALID_PRIMARY_HEALTH_TRIBUNAL_LIST_SUBS_EMAIL),
            Arguments.of("Crown Daily List", VALID_CROWN_DAILY_LIST_SUBS_EMAIL)
        );
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
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(missingArtefactIdJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidSubscriptionCriteriaForSubscriptionReturnsBadRequest() throws Exception {

        String invalidSubscriptionJsonBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[]},"
                + "\"artefactId\": \"12d0ea1e-d7bc-11ec-9d64-0242ac120002\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(invalidSubscriptionJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidFlatFileRequest() throws Exception {
        String validBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"9\"]},"
                + "\"artefactId\": \"79f5c9ae-a951-44b5-8856-3ad6b7454b0e\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(validBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testValidFlatFileRequestCsv() throws Exception {
        String validBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"4\"]},"
                + "\"artefactId\": \"8545507a-e985-4931-bba2-76be0e6ac396\"}";

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
                            .content(THIRD_PARTY_SUBSCRIPTION_ARTEFACT_BODY)
                            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(content()
                           .string(containsString("Successfully sent empty list to https://localhost:4444")));

        // Assert expected request headers sent to third party api
        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        Map<String, String> headers = Map.ofEntries(
            Map.entry("x-provenance", "MANUAL_UPLOAD"),
            Map.entry("x-type", "LIST"),
            Map.entry("x-list-type", "CIVIL_DAILY_CAUSE_LIST"),
            Map.entry("x-content-date", "2022-06-09T07:36:35"),
            Map.entry("x-sensitivity", "PUBLIC"),
            Map.entry("x-language", "ENGLISH"),
            Map.entry("x-display-from", "2022-02-16T07:36:35"),
            Map.entry("x-display-to", "2099-06-02T07:36:35"),
            Map.entry("x-location-name", "Reading County Court and Family Court"),
            Map.entry("x-location-jurisdiction", "Family,Civil"),
            Map.entry("x-location-region", "South East")
        );

        headers.entrySet().stream().forEach(e -> {
            assertThat(recordedRequest.getHeader(e.getKey()))
                .as("Incorrect header " + e.getKey())
                .isEqualTo(e.getValue());
        });
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

    @Test
    void testValidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Media user verification email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadInactiveUserNotificationEmail() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Inactive user sign-in notification email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadMInactiveUserNotificationEmail() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
