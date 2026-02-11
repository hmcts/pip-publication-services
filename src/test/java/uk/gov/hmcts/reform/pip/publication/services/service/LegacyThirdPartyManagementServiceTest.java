package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscriptionArtefact;

import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LegacyThirdPartyManagementServiceTest {
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

    @InjectMocks
    private LegacyThirdPartyManagementService legacyThirdPartyManagementService;

    @Mock
    private DataManagementService dataManagementService;

    @Mock
    private LegacyThirdPartyService legacyThirdPartyService;

    @BeforeAll
    static void setup() {
        ARTEFACT.setArtefactId(RAND_UUID);
        ARTEFACT.setLocationId(LOCATION_ID.toString());
        LOCATION.setLocationId(LOCATION_ID);
        LOCATION.setName(LOCATION_NAME);
    }

    @Test
    void testHandleThirdPartyFlatFile() {
        ARTEFACT.setListType(ListType.COP_DAILY_CAUSE_LIST);
        ARTEFACT.setIsFlatFile(true);

        byte[] file = new byte[10];
        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactFlatFile(RAND_UUID)).thenReturn(file);
        when(legacyThirdPartyService.handleFlatFileThirdPartyCall(API_DESTINATION, file, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        LegacyThirdPartySubscription subscription = new LegacyThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, legacyThirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with flat file should return successful referenceId.");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testHandleThirdPartyJson(ListType listType, Language language, boolean expectedAdditionalPdf) {
        ARTEFACT.setListType(listType);
        ARTEFACT.setLanguage(language);
        ARTEFACT.setIsFlatFile(false);
        LOCATION.setName(LOCATION_NAME);

        String jsonPayload = "test";
        byte[] pdfInBytes = "Test byte".getBytes();
        String base64EncodedPdf = Base64.getEncoder().encodeToString(pdfInBytes);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(RAND_UUID)).thenReturn(jsonPayload);
        when(dataManagementService.getArtefactFile(eq(RAND_UUID), any(), anyBoolean())).thenReturn(base64EncodedPdf);
        when(legacyThirdPartyService.handleJsonThirdPartyCall(API_DESTINATION, jsonPayload, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);
        when(legacyThirdPartyService.handlePdfThirdPartyCall(API_DESTINATION, pdfInBytes, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        LegacyThirdPartySubscription subscription = new LegacyThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, legacyThirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with json file should return successful referenceId.");

        verify(dataManagementService).getArtefactFile(RAND_UUID, FileType.PDF, expectedAdditionalPdf);
        verify(legacyThirdPartyService).handlePdfThirdPartyCall(API_DESTINATION, pdfInBytes, ARTEFACT, LOCATION);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(ListType.SJP_PRESS_LIST, Language.ENGLISH, false),
            Arguments.of(ListType.SJP_PRESS_LIST, Language.WELSH, false),
            Arguments.of(ListType.COP_DAILY_CAUSE_LIST, Language.ENGLISH, false),
            Arguments.of(ListType.COP_DAILY_CAUSE_LIST, Language.WELSH, true)
        );
    }

    @Test
    void testHandleThirdPartyJsonWithEmptyPdf() {
        ARTEFACT.setListType(ListType.COP_DAILY_CAUSE_LIST);
        ARTEFACT.setLanguage(Language.ENGLISH);
        ARTEFACT.setIsFlatFile(false);
        LOCATION.setName(LOCATION_NAME);

        String jsonPayload = "test";
        byte[] pdfInBytes = new byte[0];
        String base64EncodedPdf = Base64.getEncoder().encodeToString(pdfInBytes);

        when(dataManagementService.getArtefact(RAND_UUID)).thenReturn(ARTEFACT);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(dataManagementService.getArtefactJsonBlob(RAND_UUID)).thenReturn(jsonPayload);
        when(dataManagementService.getArtefactFile(RAND_UUID, FileType.PDF, false)).thenReturn(base64EncodedPdf);
        when(legacyThirdPartyService.handleJsonThirdPartyCall(API_DESTINATION, jsonPayload, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        LegacyThirdPartySubscription subscription = new LegacyThirdPartySubscription();
        subscription.setArtefactId(RAND_UUID);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(SUCCESS_API_SENT, legacyThirdPartyManagementService.handleThirdParty(subscription),
                     "Api subscription with json file should return successful referenceId.");
        verify(legacyThirdPartyService, never()).handlePdfThirdPartyCall(
            API_DESTINATION, pdfInBytes, ARTEFACT, LOCATION
        );
    }


    @Test
    void testHandleThirdPartyEmpty() {
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(LOCATION);
        when(legacyThirdPartyService.handleDeleteThirdPartyCall(API_DESTINATION, ARTEFACT, LOCATION))
            .thenReturn(SUCCESS_REF_ID);

        LegacyThirdPartySubscriptionArtefact subscription = new LegacyThirdPartySubscriptionArtefact();
        subscription.setArtefact(ARTEFACT);
        subscription.setApiDestination(API_DESTINATION);

        assertEquals(EMPTY_API_SENT,
                     legacyThirdPartyManagementService.notifyThirdPartyForArtefactDeletion(subscription),
                     MESSAGES_MATCH);
    }
}
