package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import org.apache.http.entity.ContentType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactType;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.service.DataManagementService;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.ExcessiveImports"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
class NotifyThirdPartyTest extends RedisConfigurationTestBase {
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
                "contentDate": "2025-01-08T07:36:35",
                "sensitivity": "PUBLIC",
                "language": "ENGLISH",
                "displayFrom": "2025-01-08T00:00:00",
                "displayTo": "2025-01-09T00:00:00"
            }
        }
        """;

    private static final String API_SUBSCRIPTION_URL = "/notify/api";
    private static final String EXTERNAL_PAYLOAD = "test";

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
    private static final String CONTENT_TYPE = "Content-Type";

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME
        ));

    String validMediaReportingJson;
    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();

    String validLocationsListJson;
    private static final String THIRD_PARTY_FAIL_MESSAGE = "Third party request to: https://localhost:4444 "
        + "failed after 3 retries due to: 404 Not Found from POST https://localhost:4444";

    private static final byte[] FILE = "Test byte".getBytes();
    private static final String LOCATION_ID = "999";
    private static final String LOCATION_NAME = "Test court";

    private final Artefact artefact = new Artefact();
    private final Location location = new Location();

    private MockWebServer externalApiMockServer;

    @MockBean
    protected DataManagementService dataManagementService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        artefact.setLocationId(LOCATION_ID);
        artefact.setType(ArtefactType.LIST);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setSensitivity(Sensitivity.PUBLIC);
        artefact.setLanguage(Language.ENGLISH);
        artefact.setContentDate(DATE_TIME);
        artefact.setDisplayFrom(DATE_TIME);
        artefact.setDisplayTo(DATE_TIME.plusDays(1));

        location.setLocationId(Integer.parseInt(LOCATION_ID));
        location.setName(LOCATION_NAME);
        location.setJurisdiction(List.of("Civil", "Family"));
        location.setRegion(List.of("South East"));

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

        lenient().when(dataManagementService.getArtefact(any())).thenReturn(artefact);
        lenient().when(dataManagementService.getLocation(any())).thenReturn(location);
        lenient().when(dataManagementService.getArtefactFlatFile(any())).thenReturn(FILE);
        lenient().when(dataManagementService.getArtefactJsonBlob(any())).thenReturn("Test JSON");

        String base64EncodedFile = Base64.getEncoder().encodeToString(FILE);
        lenient().when(dataManagementService.getArtefactFile(any(), eq(FileType.PDF), anyBoolean()))
            .thenReturn(base64EncodedFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        externalApiMockServer.close();
    }

    @Test
    void testNotifyApiSubscribersJson() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA)
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
                                          .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                          .setBody(EXTERNAL_PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA)
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
            recordedRequest.getBody().readByteArray()), recordedRequest.getHeader(CONTENT_TYPE)
                                                                  .split("=")[1].getBytes(), 1024, null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        multipartStream.readHeaders();
        multipartStream.readBodyData(output);
        try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
            assertThat(document.getNumberOfPages()).as("Incorrect number of pages").isEqualTo(1);
        }
    }

    @Test
    void testNotifyApiSubscribersForArtefactDeletion() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .setResponseCode(200));

        mockMvc.perform(put(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_ARTEFACT_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent empty list to https://localhost:4444")));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        assertThat(recordedRequest.getHeader("x-provenance"))
            .as("Incorrect provenance")
            .isEqualTo("MANUAL_UPLOAD");

        checkDataForThirdPartyRequest(recordedRequest);

        assertThat(new String(recordedRequest.getBody().readByteArray()))
            .as("Incorrect body")
            .isEmpty();
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendThirdPartySubscription() throws Exception {
        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(THIRD_PARTY_SUBSCRIPTION_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    //    @Test
    //    void testNotifyApiSubscribersFile() throws Exception {
    //        externalApiMockServer.enqueue(new MockResponse()
    //                                          .addHeader(CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA)
    //                                          .setBody(EXTERNAL_PAYLOAD)
    //                                          .setResponseCode(200));
    //
    //        mockMvc.perform(post(API_SUBSCRIPTION_URL)
    //                            .content(THIRD_PARTY_SUBSCRIPTION_FILE_BODY)
    //                            .contentType(MediaType.APPLICATION_JSON))
    //            .andExpect(status().isOk()).andExpect(content().string(containsString(
    //                "Successfully sent list to https://localhost:4444")));
    //
    //        // Assert request body sent to third party api
    //        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
    //        assertThat(recordedRequest.getHeader(CONTENT_TYPE))
    //            .as("Incorrect content type in request header")
    //            .contains(MediaType.MULTIPART_FORM_DATA_VALUE);
    //
    //        assertThat(recordedRequest.getBody().readUtf8())
    //            .as("Expected data missing in request body")
    //            .isNotNull()
    //            .isNotEmpty()
    //            .contains("\"publicationDate\": \"2022-03-25T23:30:52.123Z\"");
    //    }

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

    private void checkDataForThirdPartyRequest(RecordedRequest recordedRequest) {
        assertThat(recordedRequest.getMethod()).as("Incorrect method").isEqualTo("POST");
        assertThat(recordedRequest.getHeader("x-type")).as("Incorrect type").isEqualTo("LIST");
        assertThat(recordedRequest.getHeader("x-list-type")).as("Incorrect list type")
            .isEqualTo(ListType.CIVIL_DAILY_CAUSE_LIST.toString());
        assertThat(recordedRequest.getHeader("x-location-name")).as("Incorrect location name")
            .isEqualTo(LOCATION_NAME);
        assertThat(recordedRequest.getHeader("x-location-jurisdiction"))
            .as("Incorrect location jurisdiction")
            .isEqualTo("Civil,Family");
        assertThat(recordedRequest.getHeader("x-location-region")).as("Incorrect location region")
            .isEqualTo("South East");
        assertThat(recordedRequest.getHeader("x-content-date")).as("Incorrect content date")
            .isEqualTo("2025-01-08T07:36:35");
        assertThat(recordedRequest.getHeader("x-sensitivity")).as("Incorrect sensitivity")
            .isEqualTo("PUBLIC");
        assertThat(recordedRequest.getHeader("x-language")).as("Incorrect language")
            .isEqualTo("ENGLISH");
        assertThat(recordedRequest.getHeader("x-display-from")).as("Incorrect display from")
            .isEqualTo("2025-01-08T00:00");
        assertThat(recordedRequest.getHeader("x-display-to")).as("Incorrect display to")
            .isEqualTo("2025-01-09T00:00");
    }
}
