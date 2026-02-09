package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyPublicationMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ThirdPartySubscriptionServiceTest {
    private static final UUID PUBLICATION_ID = UUID.randomUUID();
    private static final String LOCATION_NAME = "Test location name";
    private static final LocalDateTime CONTENT_DATE = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_FROM = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_TO = LocalDateTime.now().plusDays(1);
    private static final String SOURCE_ARTEFACT_ID = "TestFile.pdf";
    private static final String PAYLOAD = "{}";
    private static final byte[] FILE = new byte[]{1, 2, 3};
    private static final String FILE_NAME = PUBLICATION_ID + ".pdf";

    private static final String SUCCESS_NEW_PUBLICATION_MESSAGE =
        "Successfully sent new publication to third party subscribers";
    private static final String SUCCESS_UPDATED_PUBLICATION_MESSAGE =
        "Successfully sent updated publication to third party subscribers";
    private static final String SUCCESS_DELETED_PUBLICATION_MESSAGE =
        "Successfully sent publication deleted notification to third party subscribers";
    private static final String RESPONSE_NOT_MATCH_MESSAGE = "Returned message does not match";

    private static final Artefact ARTEFACT_JSON = new Artefact();
    private static final Artefact ARTEFACT_FLAT_FILE = new Artefact();
    private static final Artefact ARTEFACT_NO_SOURCE_ARTEFACT_ID = new Artefact();
    private static final Location LOCATION = new Location();
    private static final ThirdPartyPublicationMetadata EXPECTED_METADATA = new ThirdPartyPublicationMetadata();

    private static final ThirdPartySubscription THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION = new ThirdPartySubscription();
    private static final ThirdPartySubscription THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION =
        new ThirdPartySubscription();
    private static final ThirdPartySubscription THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION =
        new ThirdPartySubscription();
    private static final ThirdPartyOauthConfiguration OAUTH_CONFIGURATION = new ThirdPartyOauthConfiguration();

    @Mock
    private ThirdPartyApiService thirdPartyApiService;

    @Mock
    private DataManagementService dataManagementService;

    @InjectMocks
    private ThirdPartySubscriptionService thirdPartySubscriptionService;

    @BeforeAll
    static void setUp() {
        THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION.setThirdPartyOauthConfigurationList(
            List.of(new ThirdPartyOauthConfiguration())
        );
        THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION.setPublicationId(PUBLICATION_ID);
        THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION.setThirdPartyAction(ThirdPartyAction.NEW_PUBLICATION);
        THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION.setThirdPartyOauthConfigurationList(List.of(OAUTH_CONFIGURATION));

        THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION.setThirdPartyOauthConfigurationList(
            List.of(new ThirdPartyOauthConfiguration())
        );
        THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION.setPublicationId(PUBLICATION_ID);
        THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION.setThirdPartyAction(ThirdPartyAction.UPDATE_PUBLICATION);
        THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION.setThirdPartyOauthConfigurationList(List.of(OAUTH_CONFIGURATION));

        THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION.setThirdPartyOauthConfigurationList(
            List.of(new ThirdPartyOauthConfiguration())
        );
        THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION.setPublicationId(PUBLICATION_ID);
        THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION.setThirdPartyAction(ThirdPartyAction.DELETE_PUBLICATION);
        THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION.setThirdPartyOauthConfigurationList(List.of(OAUTH_CONFIGURATION));

        ARTEFACT_JSON.setArtefactId(PUBLICATION_ID);
        ARTEFACT_JSON.setIsFlatFile(false);
        ARTEFACT_JSON.setArtefactId(PUBLICATION_ID);
        ARTEFACT_JSON.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        ARTEFACT_JSON.setContentDate(CONTENT_DATE);
        ARTEFACT_JSON.setSensitivity(Sensitivity.PUBLIC);
        ARTEFACT_JSON.setLanguage(Language.ENGLISH);
        ARTEFACT_JSON.setDisplayFrom(DISPLAY_FROM);
        ARTEFACT_JSON.setDisplayTo(DISPLAY_TO);

        ARTEFACT_FLAT_FILE.setArtefactId(PUBLICATION_ID);
        ARTEFACT_FLAT_FILE.setIsFlatFile(true);
        ARTEFACT_FLAT_FILE.setArtefactId(PUBLICATION_ID);
        ARTEFACT_FLAT_FILE.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        ARTEFACT_FLAT_FILE.setContentDate(CONTENT_DATE);
        ARTEFACT_FLAT_FILE.setSensitivity(Sensitivity.PUBLIC);
        ARTEFACT_FLAT_FILE.setLanguage(Language.ENGLISH);
        ARTEFACT_FLAT_FILE.setDisplayFrom(DISPLAY_FROM);
        ARTEFACT_FLAT_FILE.setDisplayTo(DISPLAY_TO);

        EXPECTED_METADATA.setPublicationId(PUBLICATION_ID);
        EXPECTED_METADATA.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        EXPECTED_METADATA.setLocationName(LOCATION_NAME);
        EXPECTED_METADATA.setContentDate(CONTENT_DATE);
        EXPECTED_METADATA.setSensitivity(Sensitivity.PUBLIC);
        EXPECTED_METADATA.setLanguage(Language.ENGLISH);
        EXPECTED_METADATA.setDisplayFrom(DISPLAY_FROM);
        EXPECTED_METADATA.setDisplayTo(DISPLAY_TO);

        LOCATION.setName(LOCATION_NAME);
    }

    @BeforeEach
    void setUpEach() {
        when(dataManagementService.getLocation(any())).thenReturn(LOCATION);
    }

    @Test
    void testSendThirdPartySubscriptionForNewJsonPublication() {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_JSON);
        when(dataManagementService.getArtefactJsonBlob(PUBLICATION_ID)).thenReturn(PAYLOAD);

        assertThat(thirdPartySubscriptionService.sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_NEW_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactFlatFile(PUBLICATION_ID);
        verify(thirdPartyApiService).sendNewPublicationToThirdParty(
            OAUTH_CONFIGURATION, EXPECTED_METADATA, PAYLOAD,null, null
        );
    }

    @Test
    void testSendThirdPartySubscriptionForNewFlatFilePublication() {
        ARTEFACT_FLAT_FILE.setSourceArtefactId(SOURCE_ARTEFACT_ID);

        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_FLAT_FILE);
        when(dataManagementService.getArtefactFlatFile(PUBLICATION_ID)).thenReturn(FILE);

        assertThat(thirdPartySubscriptionService.sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_NEW_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactJsonBlob(PUBLICATION_ID);
        verify(thirdPartyApiService).sendNewPublicationToThirdParty(
            OAUTH_CONFIGURATION, EXPECTED_METADATA, null, FILE, FILE_NAME
        );
    }

    @Test
    void testSendThirdPartySubscriptionForUpdatedJsonPublication() {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_JSON);
        when(dataManagementService.getArtefactJsonBlob(PUBLICATION_ID)).thenReturn(PAYLOAD);

        assertThat(thirdPartySubscriptionService
                       .sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_UPDATED_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactFlatFile(PUBLICATION_ID);
        verify(thirdPartyApiService).sendUpdatedPublicationToThirdParty(
            OAUTH_CONFIGURATION, EXPECTED_METADATA, PAYLOAD,null, null
        );
    }

    @Test
    void testSendThirdPartySubscriptionForUpdatedFlatFilePublication() {
        ARTEFACT_FLAT_FILE.setSourceArtefactId(SOURCE_ARTEFACT_ID);

        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_FLAT_FILE);
        when(dataManagementService.getArtefactFlatFile(PUBLICATION_ID)).thenReturn(FILE);

        assertThat(thirdPartySubscriptionService
                       .sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_UPDATE_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_UPDATED_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactJsonBlob(PUBLICATION_ID);
        verify(thirdPartyApiService).sendUpdatedPublicationToThirdParty(
            OAUTH_CONFIGURATION, EXPECTED_METADATA, null, FILE, FILE_NAME
        );
    }

    @Test
    void testSendThirdPartySubscriptionForDeletedJsonPublication() {
        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_JSON);
        when(dataManagementService.getArtefactJsonBlob(PUBLICATION_ID)).thenReturn(PAYLOAD);

        assertThat(thirdPartySubscriptionService
                       .sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_DELETED_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactFlatFile(PUBLICATION_ID);
        verify(thirdPartyApiService).notifyThirdPartyOfPublicationDeletion(OAUTH_CONFIGURATION, PUBLICATION_ID);
    }

    @Test
    void testSendThirdPartySubscriptionForDeletedFlatFilePublication() {
        ARTEFACT_FLAT_FILE.setSourceArtefactId(SOURCE_ARTEFACT_ID);

        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_FLAT_FILE);
        when(dataManagementService.getArtefactFlatFile(PUBLICATION_ID)).thenReturn(FILE);

        assertThat(thirdPartySubscriptionService
                       .sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_DELETE_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_DELETED_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactJsonBlob(PUBLICATION_ID);
        verify(thirdPartyApiService).notifyThirdPartyOfPublicationDeletion(OAUTH_CONFIGURATION, PUBLICATION_ID);
    }

    @Test
    void testSendThirdPartySubscriptionWithoutSourceArtefactId() {
        ARTEFACT_FLAT_FILE.setSourceArtefactId(null);

        when(dataManagementService.getArtefact(PUBLICATION_ID)).thenReturn(ARTEFACT_FLAT_FILE);
        when(dataManagementService.getArtefactFlatFile(PUBLICATION_ID)).thenReturn(FILE);

        assertThat(thirdPartySubscriptionService.sendThirdPartySubscription(THIRD_PARTY_SUBSCRIPTION_NEW_PUBLICATION))
            .as(RESPONSE_NOT_MATCH_MESSAGE)
            .isEqualTo(SUCCESS_NEW_PUBLICATION_MESSAGE);

        verify(dataManagementService, never()).getArtefactJsonBlob(PUBLICATION_ID);
        verify(thirdPartyApiService).sendNewPublicationToThirdParty(
            OAUTH_CONFIGURATION, EXPECTED_METADATA, null, FILE, PUBLICATION_ID.toString()
        );
    }
}
