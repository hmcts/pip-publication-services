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
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ArtefactType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Sensitivity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
@ActiveProfiles("test")
class ThirdPartyServiceTest {

    @Autowired
    private ThirdPartyService thirdPartyService;

    @Autowired
    WebClient.Builder webClient;

    private static final String API = "localhost:4444";
    private static final String PAYLOAD = "test payload";
    private static final String SUCCESS_NOTIFICATION = "Successfully sent list to Courtel at: %s";
    private static final String RETURN_MATCH = "Returned messages should match";
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
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response,
                     RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleFlatFileThirdPartyCall(API, PAYLOAD, artefact, location);
        assertEquals(String.format(SUCCESS_NOTIFICATION, API), response,
                     RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD)
                                                    .setResponseCode(200));
        String response = thirdPartyService.handleDeleteThirdPartyCall(API, artefact, location);
        assertEquals(String.format("Successfully sent deleted notification to Courtel at: %s", API), response,
                     RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithJson() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
            thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, null, null),
                                                     "Should throw ThirdPartyException");
        assertTrue(ex.getMessage().contains(String.format("Third party request to: %s failed", API)),
                   RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
                                                         thirdPartyService.handleFlatFileThirdPartyCall(
                                                             API, PAYLOAD, null, null),
                                                     "Should throw ThirdPartyException");
        assertTrue(ex.getMessage().contains(String.format("Third party request to: %s failed", API)),
                   RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsFailedWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        ThirdPartyServiceException ex = assertThrows(ThirdPartyServiceException.class, () ->
                                                         thirdPartyService.handleDeleteThirdPartyCall(API, null, null),
                                                     "Should throw ThirdPartyException");
        assertTrue(ex.getMessage().contains(String.format("Third party request to: %s failed", API)),
                   RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithJson() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, artefact, location);
        assertEquals(String.format("Successfully sent list to Courtel at: %s", API), response,
                     RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithFlatFile() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleJsonThirdPartyCall(API, PAYLOAD, artefact, location);
        assertEquals(String.format("Successfully sent list to Courtel at: %s", API), response,
                     RETURN_MATCH);
    }

    @Test
    void testHandleCourtelCallReturnsOkAfterRetryWithDelete() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(200)
                                                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setBody(PAYLOAD));

        String response = thirdPartyService.handleDeleteThirdPartyCall(API, artefact, location);
        assertEquals(String.format("Successfully sent deleted notification to Courtel at: %s", API), response,
                     RETURN_MATCH);
    }
}
