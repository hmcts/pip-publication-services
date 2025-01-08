package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import org.apache.http.entity.ContentType;
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
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.service.DataManagementService;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String API_SUBSCRIPTION_URL = "/notify/api";

    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String CONTENT_TYPE = "Content-Type";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String API_DESTINATION = "https://localhost:4444";
    private static final String PAYLOAD = "Test JSON";
    private static final byte[] FILE = "Test byte".getBytes();
    private static final String PDF = "Test PDF";
    private static final String LOCATION_ID = "999";
    private static final String LOCATION_NAME = "Test court";

    private static final String THIRD_PARTY_FAIL_MESSAGE = "Third party request to: " + API_DESTINATION
        + " failed after 3 retries due to: 404 Not Found from POST " + API_DESTINATION;

    private final Artefact artefact = new Artefact();
    private final Location location = new Location();
    private final ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription();

    private String thirdPartySubscriptionInput;

    private MockWebServer externalApiMockServer;

    @MockBean
    protected DataManagementService dataManagementService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        OBJECT_MAPPER.findAndRegisterModules();

        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setLocationId(LOCATION_ID);
        artefact.setType(ArtefactType.LIST);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setSensitivity(Sensitivity.PUBLIC);
        artefact.setLanguage(Language.ENGLISH);
        artefact.setContentDate(DATE_TIME);
        artefact.setDisplayFrom(DATE_TIME);
        artefact.setDisplayTo(DATE_TIME.plusDays(1));
        artefact.setProvenance("MANUAL_UPLOAD");

        location.setLocationId(Integer.parseInt(LOCATION_ID));
        location.setName(LOCATION_NAME);
        location.setJurisdiction(List.of("Civil", "Family"));
        location.setRegion(List.of("South East"));

        thirdPartySubscription.setApiDestination(API_DESTINATION);
        thirdPartySubscription.setArtefactId(ARTEFACT_ID);
        thirdPartySubscriptionInput = OBJECT_MAPPER.writeValueAsString(thirdPartySubscription);

        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);

        lenient().when(dataManagementService.getArtefact(any())).thenReturn(artefact);
        lenient().when(dataManagementService.getLocation(any())).thenReturn(location);
        lenient().when(dataManagementService.getArtefactFlatFile(any())).thenReturn(FILE);
        lenient().when(dataManagementService.getArtefactJsonBlob(any())).thenReturn(PAYLOAD);
        lenient().when(dataManagementService.getArtefactFile(any(), eq(FileType.PDF), anyBoolean())).thenReturn(PDF);
    }

    @AfterEach
    void tearDown() throws IOException {
        externalApiMockServer.close();
    }

    @Test
    void testNotifyApiSubscribersJson() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                          .setBody(PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA)
                                          .setBody(PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(thirdPartySubscriptionInput)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        assertThat(recordedRequest.getHeader("x-provenance"))
            .as("Incorrect provenance")
            .isEqualTo("MANUAL_UPLOAD");
        checkDataForThirdPartyRequest(recordedRequest);

        assertThat(new String(recordedRequest.getBody().readByteArray()))
            .as("Incorrect body")
            .contains(PAYLOAD);
    }

    @Test
    void testNotifyApiSubscribersJsonGeneratedPdf() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                          .setBody(PAYLOAD)
                                          .setResponseCode(200));
        externalApiMockServer.enqueue(new MockResponse()
                                          .addHeader(CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA)
                                          .setBody(PAYLOAD)
                                          .setResponseCode(200));

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(thirdPartySubscriptionInput)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent list to https://localhost:4444")));

        externalApiMockServer.takeRequest();
        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();
        assertThat(recordedRequest.getHeader("x-provenance"))
            .as("Incorrect provenance")
            .isEqualTo("CATH");
        checkDataForThirdPartyRequest(recordedRequest);
    }

    @Test
    void testNotifyApiSubscribersForArtefactDeletion() throws Exception {
        externalApiMockServer.enqueue(new MockResponse()
                                          .setResponseCode(200));

        ThirdPartySubscriptionArtefact thirdPartySubscriptionArtefact = new ThirdPartySubscriptionArtefact();
        thirdPartySubscriptionArtefact.setApiDestination(API_DESTINATION);
        thirdPartySubscriptionArtefact.setArtefact(artefact);

        mockMvc.perform(put(API_SUBSCRIPTION_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(thirdPartySubscriptionArtefact))
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
    @WithMockUser(username = "unauthorized_username", authorities = {"APPROLE_unknown.role"})
    void testUnauthorizedSendThirdPartySubscription() throws Exception {
        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(thirdPartySubscriptionInput)
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
        when(dataManagementService.getArtefact(any())).thenThrow(ServiceToServiceException.class);

        mockMvc.perform(post(API_SUBSCRIPTION_URL)
                            .content(thirdPartySubscriptionInput)
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
                            .content(thirdPartySubscriptionInput)
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
            .isEqualTo(DATE_TIME.toString());
        assertThat(recordedRequest.getHeader("x-sensitivity")).as("Incorrect sensitivity")
            .isEqualTo("PUBLIC");
        assertThat(recordedRequest.getHeader("x-language")).as("Incorrect language")
            .isEqualTo("ENGLISH");
        assertThat(recordedRequest.getHeader("x-display-from")).as("Incorrect display from")
            .isEqualTo(DATE_TIME.toString());
        assertThat(recordedRequest.getHeader("x-display-to")).as("Incorrect display to")
            .isEqualTo(DATE_TIME.plusDays(1).toString());
    }
}
