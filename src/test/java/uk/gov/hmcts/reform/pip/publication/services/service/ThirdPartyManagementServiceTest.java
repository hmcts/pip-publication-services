package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.FileType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscriptionArtefact;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ThirdPartyManagementServiceTest {
    private static final String API_DESTINATION = "testUrl";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String SUCCESS_API_SENT = "Successfully sent list to testUrl";
    private static final String EMPTY_API_SENT = "Successfully sent empty list to testUrl";

    private static final String SUCCESS_REF_ID = "successRefId";
    private static final UUID RAND_UUID = UUID.randomUUID();
    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";

    private static final Artefact ARTEFACT = new Artefact();
    private static final Location LOCATION = new Location();

    @Autowired
    private ThirdPartyManagementService thirdPartyManagementService;

    @MockBean
    private DataManagementService dataManagementService;

    @MockBean
    private ChannelManagementService channelManagementService;

    @MockBean
    private ThirdPartyService thirdPartyService;

    @BeforeAll
    static void setup() {
        ARTEFACT.setArtefactId(RAND_UUID);
        LOCATION.setLocationId(LOCATION_ID);
        LOCATION.setName(LOCATION_NAME);
    }

    @Test
    void testHandleThirdPartyFlatFile() {
        ARTEFACT.setIsFlatFile(true);

        byte[] file = new byte[10];
        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactFlatFile(RAND_UUID)).thenReturn(file);
        when(thirdPartyService.handleFlatFileThirdPartyCall(API_DESTINATION, file, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscription subscription = new ThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, thirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with flat file should return successful referenceId.");
    }

    @Test
    void testHandleThirdPartyJson() {
        ARTEFACT.setIsFlatFile(false);
        LOCATION.setName(LOCATION_NAME);
        String jsonPayload = "test";
        byte[] pdfInBytes = "Test byte".getBytes();
        Map<FileType, byte[]> blobStorageFiles = Map.of(FileType.PDF, pdfInBytes);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(RAND_UUID)).thenReturn(jsonPayload);
        when(channelManagementService.getArtefactFiles(RAND_UUID)).thenReturn(blobStorageFiles);
        when(thirdPartyService.handleJsonThirdPartyCall(API_DESTINATION, jsonPayload, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);
        when(thirdPartyService.handlePdfThirdPartyCall(API_DESTINATION, pdfInBytes, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscription subscription = new ThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, thirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with json file should return successful referenceId.");
    }

    @Test
    void testHandleThirdPartyJsonWithEmptyPdf() {
        ARTEFACT.setIsFlatFile(false);
        LOCATION.setName(LOCATION_NAME);
        String jsonPayload = "test";
        byte[] pdfInBytes = new byte[0];
        Map<FileType, byte[]> blobStorageFiles = Map.of(FileType.PDF, pdfInBytes);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(RAND_UUID)).thenReturn(jsonPayload);
        when(channelManagementService.getArtefactFiles(RAND_UUID)).thenReturn(blobStorageFiles);
        when(thirdPartyService.handleJsonThirdPartyCall(API_DESTINATION, jsonPayload, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscription subscription = new ThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, thirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with json file should return successful referenceId.");
        verify(thirdPartyService, never()).handlePdfThirdPartyCall(API_DESTINATION, pdfInBytes, ARTEFACT, LOCATION);
    }


    @Test
    void testHandleThirdPartyEmpty() {
        when(thirdPartyService.handleDeleteThirdPartyCall(API_DESTINATION, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        ThirdPartySubscriptionArtefact subscription = new ThirdPartySubscriptionArtefact();
        subscription.setArtefact(ARTEFACT);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(EMPTY_API_SENT, thirdPartyManagementService.handleThirdParty(subscription),
                     MESSAGES_MATCH);
    }
}
