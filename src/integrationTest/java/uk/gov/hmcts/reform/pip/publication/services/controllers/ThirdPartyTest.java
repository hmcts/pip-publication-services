package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.service.thirdparty.ThirdPartyTokenCachingService;
import uk.gov.hmcts.reform.pip.publication.services.utils.IntegrationTestBase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
public class ThirdPartyTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String THIRD_PARTY_URL = "/third-party";

    private static final UUID PUBLICATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final int LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Test location name";
    private static final LocalDateTime CONTENT_DATE = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_FROM = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_TO = LocalDateTime.now().plusDays(1);

    private static final String DESTINATION_URL = "https://localhost:1111";
    private static final String TOKEN_URL = "https://localhost:2222";
    private static final String CLIENT_ID_KEY = "testClientId";
    private static final String CLIENT_SECRET_KEY = "testClientSecret";
    private static final String SCOPE_KEY = "testScope";
    private static final String PAYLOAD = "Test payload";
    private static final String CONTENT_TYPE = "Content-Type";

    private static final String METHOD_MATCH_MESSAGE = "Request method does not match";
    private static final String HEADER_MATCH_MESSAGE = "Request header does not match";
    private static final String BODY_MATCH_MESSAGE = "Request body does not match";

    private static final Artefact ARTEFACT = new Artefact();
    private static final Location LOCATION = new Location();
    private static final  ThirdPartyOauthConfiguration THIRD_PARTY_OAUTH_CONFIGURATION =
        new ThirdPartyOauthConfiguration();
    private static final ThirdPartySubscription THIRD_PARTY_SUBSCRIPTION = new ThirdPartySubscription();

    private MockWebServer externalApiMockServer;

    @MockitoBean
    private ThirdPartyTokenCachingService thirdPartyTokenCachingService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void setUp() {
        ARTEFACT.setArtefactId(PUBLICATION_ID);
        ARTEFACT.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        ARTEFACT.setContentDate(CONTENT_DATE);
        ARTEFACT.setLocationId(String.valueOf(LOCATION_ID));
        ARTEFACT.setSensitivity(Sensitivity.PUBLIC);
        ARTEFACT.setLanguage(Language.ENGLISH);
        ARTEFACT.setDisplayFrom(DISPLAY_FROM);
        ARTEFACT.setDisplayTo(DISPLAY_TO);

        LOCATION.setLocationId(LOCATION_ID);
        LOCATION.setName(LOCATION_NAME);

        THIRD_PARTY_OAUTH_CONFIGURATION.setUserId(USER_ID);
        THIRD_PARTY_OAUTH_CONFIGURATION.setDestinationUrl(DESTINATION_URL);
        THIRD_PARTY_OAUTH_CONFIGURATION.setTokenUrl(TOKEN_URL);
        THIRD_PARTY_OAUTH_CONFIGURATION.setClientIdKey(CLIENT_ID_KEY);
        THIRD_PARTY_OAUTH_CONFIGURATION.setClientSecretKey(CLIENT_SECRET_KEY);
        THIRD_PARTY_OAUTH_CONFIGURATION.setScopeKey(SCOPE_KEY);

        THIRD_PARTY_SUBSCRIPTION.setThirdPartyOauthConfigurationList(List.of(THIRD_PARTY_OAUTH_CONFIGURATION));
        THIRD_PARTY_SUBSCRIPTION.setPublicationId(PUBLICATION_ID);
    }

    @BeforeEach
    void setup() throws IOException {
        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);

        when(thirdPartyTokenCachingService.getCachedToken(any())).thenReturn("testAccessToken");
    }

    @AfterEach
    void tearDown() throws IOException {
        externalApiMockServer.close();
    }

    @Test
    void testSendNewPublicationToThirdParty() throws Exception {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(String.valueOf(LOCATION_ID))).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(PUBLICATION_ID)).thenReturn(PAYLOAD);

        externalApiMockServer.enqueue(new MockResponse().setResponseCode(200));

        THIRD_PARTY_SUBSCRIPTION.setThirdPartyAction(ThirdPartyAction.NEW_PUBLICATION);
        mockMvc.perform(post(THIRD_PARTY_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(THIRD_PARTY_SUBSCRIPTION))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Successfully sent new publication to third party subscribers"
            )));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(recordedRequest.getMethod())
            .as(METHOD_MATCH_MESSAGE)
            .isEqualTo("POST");

        softly.assertThat(recordedRequest.getHeader(CONTENT_TYPE))
            .as(HEADER_MATCH_MESSAGE)
            .isEqualTo(MediaType.MULTIPART_FORM_DATA.toString());

        softly.assertAll();
    }

    @Test
    void testSendUpdatedPublicationToThirdParty() throws Exception {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(String.valueOf(LOCATION_ID))).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(PUBLICATION_ID)).thenReturn(PAYLOAD);

        externalApiMockServer.enqueue(new MockResponse().setResponseCode(200));

        THIRD_PARTY_SUBSCRIPTION.setThirdPartyAction(ThirdPartyAction.UPDATE_PUBLICATION);
        mockMvc.perform(post(THIRD_PARTY_URL)
                        .content(OBJECT_MAPPER.writeValueAsString(THIRD_PARTY_SUBSCRIPTION))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Successfully sent updated publication to third party subscribers"
                )));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(recordedRequest.getMethod())
                .as(METHOD_MATCH_MESSAGE)
                .isEqualTo("PUT");

        softly.assertThat(recordedRequest.getHeader(CONTENT_TYPE))
                .as(HEADER_MATCH_MESSAGE)
                .isEqualTo(MediaType.MULTIPART_FORM_DATA.toString());

        softly.assertAll();
    }

    @Test
    void testSendDeletedPublicationNotificationToThirdParty() throws Exception {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(null);

        externalApiMockServer.enqueue(new MockResponse().setResponseCode(200));

        THIRD_PARTY_SUBSCRIPTION.setThirdPartyAction(ThirdPartyAction.DELETE_PUBLICATION);
        mockMvc.perform(post(THIRD_PARTY_URL)
                        .content(OBJECT_MAPPER.writeValueAsString(THIRD_PARTY_SUBSCRIPTION))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Successfully sent publication deleted notification to third party subscribers"
                )));

        RecordedRequest recordedRequest = externalApiMockServer.takeRequest();

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(recordedRequest.getMethod())
                .as(METHOD_MATCH_MESSAGE)
                .isEqualTo("DELETE");

        softly.assertAll();
    }

    @Test
    @WithMockUser(username = "unauthorized_username", authorities = {"APPROLE_unknown.role"})
    void testUnauthorizedSendThirdPartySubscription() throws Exception {
        mockMvc.perform(post(THIRD_PARTY_URL)
                        .content(OBJECT_MAPPER.writeValueAsString(THIRD_PARTY_SUBSCRIPTION))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
