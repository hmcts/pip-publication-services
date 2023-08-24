package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class ThirdPartyManagementService {
    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s";
    private static final String EMPTY_SUCCESS_MESSAGE = "Successfully sent empty list to %s";

    @Autowired
    private DataManagementService dataManagementService;

    @Autowired
    private ChannelManagementService channelManagementService;

    @Autowired
    private ThirdPartyService thirdPartyService;

    /**
     * Handles the incoming request for sending lists out to third party publishers, uses the artefact id from body
     * to retrieve Artefact from Data Management and then gets the file or json payload to then send out.
     *
     * @param body Request body of ThirdParty subscription containing artefact id and the destination api.
     * @return String of successful POST.
     */
    public String handleThirdParty(ThirdPartySubscription body) {
        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        Location location = dataManagementService.getLocation(artefact.getLocationId());
        if (artefact.getIsFlatFile().equals(Boolean.TRUE)) {
            log.info(writeLog(thirdPartyService.handleFlatFileThirdPartyCall(
                body.getApiDestination(), dataManagementService.getArtefactFlatFile(artefact.getArtefactId()),
                artefact, location)));
        } else {
            handleThirdPartyForJson(body.getApiDestination(), artefact, location);
        }
        return String.format(SUCCESS_MESSAGE, body.getApiDestination());
    }

    /**
     * Handles the incoming request for sending out an empty list to third party API with the deleted artefact
     * information in the request headers.
     *
     * @param body Request body of ThirdParty subscription containing the deleted artefact and the destination api.
     * @return String of successful PUT.
     */
    public String notifyThirdPartyForArtefactDeletion(ThirdPartySubscriptionArtefact body) {
        Artefact artefact = body.getArtefact();
        Location location = dataManagementService.getLocation(artefact.getLocationId());

        log.info(writeLog(thirdPartyService.handleDeleteThirdPartyCall(body.getApiDestination(),
                                                                       artefact,
                                                                       location)));
        return String.format(EMPTY_SUCCESS_MESSAGE, body.getApiDestination());
    }

    private void handleThirdPartyForJson(String api, Artefact artefact, Location location) {
        String jsonBlob = dataManagementService.getArtefactJsonBlob(artefact.getArtefactId());
        log.info(writeLog(thirdPartyService.handleJsonThirdPartyCall(api, jsonBlob, artefact, location)));

        byte[] pdf = channelManagementService.getArtefactFile(artefact.getArtefactId(), FileType.PDF).getBytes();
        if (pdf.length == 0) {
            log.warn(writeLog("Empty PDF not sent to third party"));
        } else {
            // The PDF returned from channel management is returned as Base 64.
            // This is then decoded here before sending to third parties.
            log.info(writeLog(thirdPartyService.handlePdfThirdPartyCall(
                api, Base64.decodeBase64(pdf), artefact, location)));
        }
    }
}
