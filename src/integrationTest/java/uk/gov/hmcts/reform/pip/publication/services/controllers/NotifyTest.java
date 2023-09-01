package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import org.apache.http.entity.ContentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.junit.jupiter.api.AfterEach;
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
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING = """
        {
            "email": "test@email.com",
            "isExisting": "true",
            "fullName": "fullName"
        }
        """;
    private static final String VALID_WELCOME_REQUEST_BODY_NEW = """
        {
            "email": "test@email.com",
            "isExisting": "false",
            "fullName": "fullName"
        }
        """;
    private static final String VALID_ADMIN_CREATION_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "surname": "surname",
            "forename": "forename"
        };
        """;
    private static final String INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "isExisting":
        }
        """;
    private static final String VALID_DUPLICATE_MEDIA_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "fullName": "fullName"
        };
        """;
    private static final String DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "fullName":
        }
        """;
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";
    private static final String MEDIA_REPORTING_EMAIL_URL = "/notify/media/report";
    private static final String MI_REPORTING_EMAIL_URL = "/notify/mi/report";
    private static final String THIRD_PARTY_SUBSCRIPTION_JSON_BODY = """
        {
            "apiDestination": "https://localhost:4444",
            "artefactId": "3d498688-bbad-4a53-b253-a16ddf8737a9"
        }
        """;
    private static final String THIRD_PARTY_SUBSCRIPTION_FILE_BODY = """
        {
            "apiDestination": "https://localhost:4444",
            "artefactId": "f9e659e3-4584-4f15-859d-174ce4b48cbb"
        }
        """;
    private static final String THIRD_PARTY_SUBSCRIPTION_INVALID_ARTEFACT_BODY = """
        {
            "apiDestination": "http://localhost:4444",
            "artefactId": "1e565487-23e4-4a25-9364-43277a5180d4"
        }
        """;
    private static final String THIRD_PARTY_SUBSCRIPTION_ARTEFACT_BODY = """
        {
            "apiDestination": "https://localhost:4444",
            "artefact": {
                "artefactId": "70494df0-31c1-4290-bbd2-7bfe7acfeb81",
                "listType": "CIVIL_DAILY_CAUSE_LIST",
                "locationId": "2",
                "provenance": "MANUAL_UPLOAD",
                "type": "LIST",
                "contentDate": "2022-06-09T07:36:35",
                "sensitivity": "PUBLIC",
                "language": "ENGLISH",
                "displayFrom": "2022-02-16T07:36:35",
                "displayTo": "2099-06-02T07:36:35"
            }
        }
        """;
    private static final String API_SUBSCRIPTION_URL = "/notify/api";
    private static final String EXTERNAL_PAYLOAD = "test";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = "/notify/media/verification";
    private static final String MEDIA_REJECTION_EMAIL_URL = "/notify/media/reject";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = "/notify/user/sign-in";

    private static final String NOTIFY_SYSTEM_ADMIN_URL = "/notify/sysadmin/update";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL = "/notify/location-subscription-delete";

    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_username";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.role";

    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY = """
        {
            "locationName": "Test Location",
            "subscriberEmails": [
                "test.system.admin@justice.gov.uk"
            ]
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY = """
        {
            "requesterName": "reqName",
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;
    private static final String NONEXISTENT_BLOB_SUBS_EMAIL = """
        {
            "artefactId": "b190522a-5d9b-4089-a8c8-6918721c93df",
            "email": "test_account_admin@justice.gov.uk",
            "subscriptions": {
                "CASE_URN": [
                    "123"
                ]
            }
        }
        """;
    private static final String VALID_MEDIA_VERIFICATION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@email.com"
        }
        """;

    private static final String VALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String INVALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": "invalid"
        }
         """;

    private static final String INVALID_NOTIFY_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY = """
        {
            "email": "test@test.com",
            "fullName": "testName",
            "lastSignedInDate": "01 May 2022"
        }
        """;
    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME
        ));

    String validMediaReportingJson;
    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();

    String validLocationsListJson;

    private static final String SUBSCRIPTION_URL = "/notify/subscription";
    private static final String DUPLICATE_MEDIA_EMAIL_URL = "/notify/duplicate/media";
    private static final String THIRD_PARTY_FAIL_MESSAGE = "Third party request to: https://localhost:4444 "
        + "failed after 3 retries due to: 404 Not Found from POST https://localhost:4444";

    private MockWebServer externalApiMockServer;

    @Autowired
    private MockMvc mockMvc;


    @BeforeEach
    void setup() throws IOException {

        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(
            UUID.randomUUID(),
            "TEST",
            "1234"
        ));

        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

        validMediaReportingJson = ow.writeValueAsString(MEDIA_APPLICATION_LIST);
        validLocationsListJson = ow.writeValueAsString(NO_MATCH_ARTEFACT_LIST);
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
    void testSendDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Duplicate media account email successfully sent")));
    }

    @Test
    void testSendDuplicateMediaAccountEmailBadRequest() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }


    @Test
    void testSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Created admin welcome email successfully sent with referenceId")));
    }

    @Test
    void testSendAdminAccountWelcomeEmailBadRequest() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testNotifyApiSubscribersJson() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type", ContentType.APPLICATION_JSON)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type", ContentType.MULTIPART_FORM_DATA)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        assertThat(recordedRequest.getHeader("x-provenance")).as("Incorrect provenance").isEqualTo("MANUAL_UPLOAD");
        checkDataForThirdPartyRequest(recordedRequest);

        assertThat(new String(recordedRequest.getBody().readByteArray()))
            .as("Incorrect body")
            .contains("publicationDate");
    }

    @Test
    void testNotifyApiSubscribersJsonGeneratedPdf() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type", ContentType.APPLICATION_JSON)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type", ContentType.MULTIPART_FORM_DATA)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));

        externalApiMockServer.takeRequest();
        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        assertThat(recordedRequest.getHeader("x-provenance")).as("Incorrect provenance").isEqualTo("CATH");
        checkDataForThirdPartyRequest(recordedRequest);

        MultipartStream multipartStream = new MultipartStream(new ByteArrayInputStream(
            recordedRequest.getBody().readByteArray()), recordedRequest.getHeader("Content-Type")
            .split("=")[1].getBytes(), 1024, null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        multipartStream.readHeaders();
        multipartStream.readBodyData(output);
        try (PDDocument document = PDDocument.load(output.toByteArray())) {
            assertThat(document.getNumberOfPages()).as("Incorrect number of pages").isEqualTo(1);
        }

    }

    private void checkDataForThirdPartyRequest(RecordedRequest recordedRequest) {
        assertThat(recordedRequest.getMethod()).as("Incorrect method").isEqualTo("POST");
        assertThat(recordedRequest.getHeader("x-type")).as("Incorrect type").isEqualTo("LIST");
        assertThat(recordedRequest.getHeader("x-list-type")).as("Incorrect list type")
            .isEqualTo("SJP_PUBLIC_LIST");
        assertThat(recordedRequest.getHeader("x-location-name")).as("Incorrect location name")
            .isEqualTo("AB - E2E TEST COURT (BACKEND) - DO NOT REMOVE");
        assertThat(recordedRequest.getHeader("x-location-jurisdiction"))
            .as("Incorrect location jurisdiction")
            .isEqualTo("Family,Civil");
        assertThat(recordedRequest.getHeader("x-location-region")).as("Incorrect location region")
            .isEqualTo("South East");
        assertThat(recordedRequest.getHeader("x-content-date")).as("Incorrect content date")
            .isEqualTo("2023-03-01T00:00");
        assertThat(recordedRequest.getHeader("x-sensitivity")).as("Incorrect sensitivity")
            .isEqualTo("PUBLIC");
        assertThat(recordedRequest.getHeader("x-language")).as("Incorrect language")
            .isEqualTo("ENGLISH");
        assertThat(recordedRequest.getHeader("x-display-from")).as("Incorrect display from")
            .isEqualTo("2022-10-10T10:40");
        assertThat(recordedRequest.getHeader("x-display-to")).as("Incorrect display to")
            .isEqualTo("2030-12-31T10:55:12");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendThirdPartySubscription() throws Exception {
        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testNotifyApiSubscribersFile() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader("Content-Type", ContentType.MULTIPART_FORM_DATA)
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
            .contains("\"publicationDate\": \"2022-03-25T23:30:52.123Z\"");
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
    void testSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testSendMediaReportingEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testMissingEmailForSubscriptionReturnsBadRequest() throws Exception {

        String missingEmailJsonBody =
            "{\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}, \"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

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
    void testInvalidEmailForSubscriptionReturnsBadRequest() throws Exception {

        String invalidEmailJsonBody =
            "{\"email\":\"abcd\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]},"
                + "\"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

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
                + "\"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(invalidSubscriptionJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidFlatFileRequest() throws Exception {
        String validBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"998\"]},"
                + "\"artefactId\": \"55995355-466b-4991-a7da-9d016cbaa591\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(validBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testValidFlatFileRequestCsv() throws Exception {
        String validBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"998\"]},"
                + "\"artefactId\": \"7ace17ef-0e5c-4db7-ae4a-a7d7953e0073\"}";

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(validBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendSubscriptionEmail() throws Exception {
        String validBody = """
            {
                "artefactId": "3d498688-bbad-4a53-b253-a16ddf8737a9",
                "email": "test_account_admin@justice.gov.uk",
                "subscriptions": {
                    "LOCATION_ID": [
                        "4"
                    ]
                }
            }
            """;

        mockMvc.perform(post(SUBSCRIPTION_URL)
                            .content(validBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
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
    void testSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andExpect(content().string(
                containsString("Unidentified blob email successfully sent with reference id:")));
    }

    @Test
    void testSendUnidentifiedBlobEmailBadRequest() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
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
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaUserVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMediaUserRejectionEmail() throws Exception {

        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Media user rejection email successfully sent with referenceId")));
    }

    @Test
    void testSendMediaUserRejectionEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(INVALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMediaUserRejectionEmailNotifyBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(INVALID_NOTIFY_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testSendMediaUserRejectionEmailUnauthorized() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Inactive user sign-in notification email successfully sent with referenceId")));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsersBadRequest() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMiReportingEmail() throws Exception {
        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMiReportingEmail() throws Exception {
        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Send notification email successfully to all system admin with referenceId")));
    }

    @Test
    void testSendSystemAdminBadPayload() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content("content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Location subscription email successfully sent with reference id")));
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailBadRequest() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content("invalid content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
