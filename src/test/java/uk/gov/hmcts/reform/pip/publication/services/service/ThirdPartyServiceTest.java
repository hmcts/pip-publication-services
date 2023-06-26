package uk.gov.hmcts.reform.pip.publication.services.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
class ThirdPartyServiceTest {

    @Autowired
    private ThirdPartyService thirdPartyService;

    @Autowired
    WebClient.Builder webClient;

    private static final String API = "http://localhost:4444";
    private static final String PAYLOAD = "test payload";
    private static final byte[] BYTE_ARRAY_PAYLOAD = {1, 2, 3};
    private static final String SUCCESS_NOTIFICATION = "Successfully sent list to Courtel at: %s";
    private static final String DELETE_SUCCESS_NOTIFICATION =
        "Successfully sent deleted notification to Courtel at: %s";
    private static final String PDF_SUCCESS_NOTIFICATION = "Successfully sent PDF to Courtel at: %s";
    private static final String FAILED_REQUEST_NOTIFICATION = "Third party request to: %s failed";
    private static final String RETURN_MATCH = "Returned messages should match";
    private static final String EXCEPTION_MESSAGE = "Should throw ThirdPartyException";
    private static MockWebServer mockPublicationServicesEndpoint;

    private final Artefact artefact = new Artefact();
    private final Location location = new Location();
    private static final LocalDateTime TODAY_DATE = LocalDateTime.now().toLocalDate().atStartOfDay();

    @BeforeEach
    void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(4444);

        artefact.setProvenance("Provenance");
        artefact.setSourceArtefactId("SourceArtefactId");
        artefact.setType(ArtefactType.GENERAL_PUBLICATION);
        artefact.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact.setContentDate(TODAY_DATE);
        artefact.setSensitivity(Sensitivity.PUBLIC);
        artefact.setLanguage(Language.ENGLISH);
        artefact.setDisplayFrom(TODAY_DATE);
        artefact.setDisplayTo(TODAY_DATE);

        location.setName("Location Name");
        location.setRegion(List.of("Venue Region A"));
        location.setJurisdiction(List.of("Venue Jurisdiction A"));
    }

    @AfterEach
    void after() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testHandleCourtelCallReturnsOkWithJson() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, artefact, location);
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleFlatFileThirdPartyCall(API, BYTE_ARRAY_PAYLOAD, artefact, location);
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleDeleteThirdPartyCall(API, artefact, location);
        assertEquals(String.format(DELETE_SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkWhenSendingPdf() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handlePdfThirdPartyCall(API, BYTE_ARRAY_PAYLOAD, artefact, location);
        assertEquals(String.format(PDF_SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithJson() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
            thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, null, null), EXCEPTION_MESSAGE);
        assertTrue(ex.getMessage().contains(String.format(FAILED_REQUEST_NOTIFICATION, API)), RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
                                                         thirdPartyService.handleFlatFileThirdPartyCall(
                                                             API, BYTE_ARRAY_PAYLOAD, artefact, null),
                                                     EXCEPTION_MESSAGE);
        assertTrue(ex.getMessage().contains(String.format(FAILED_REQUEST_NOTIFICATION, API)), RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
                                                         thirdPartyService.handleDeleteThirdPartyCall(API, null, null),
                                                     EXCEPTION_MESSAGE);
        assertTrue(ex.getMessage().contains(String.format(FAILED_REQUEST_NOTIFICATION, API)), RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWhenSendingPdf() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
                                                         thirdPartyService.handlePdfThirdPartyCall(
                                                             API, BYTE_ARRAY_PAYLOAD, artefact, null),
                                                     EXCEPTION_MESSAGE);
        assertTrue(ex.getMessage().contains(String.format(FAILED_REQUEST_NOTIFICATION, API)), RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithJson() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, artefact, location);
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleFlatFileThirdPartyCall(API, BYTE_ARRAY_PAYLOAD, artefact, location);
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleDeleteThirdPartyCall(API, artefact, location);
        assertEquals(String.format(DELETE_SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWhenSendingPdf() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handlePdfThirdPartyCall(API, BYTE_ARRAY_PAYLOAD, artefact, location);
        assertEquals(String.format(PDF_SUCCESS_NOTIFICATION, API), response, RETURN_MATCH);
    }
}
