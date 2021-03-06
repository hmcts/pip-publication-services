package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class NotificationService {

    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s";
    private static final String EMPTY_SUCCESS_MESSAGE = "Successfully sent empty list to %s";

    @Autowired
    private EmailService emailService;

    @Autowired
    private CsvCreationService csvCreationService;
    @Autowired
    private DataManagementService dataManagementService;

    @Autowired
    private ThirdPartyService thirdPartyService;

    /**
     * Handles the incoming request for welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and isExisting values e.g.
     *             {email: 'example@email.com', isExisting: 'true'}
     */
    public String handleWelcomeEmailRequest(WelcomeEmail body) {
        log.info(writeLog(String.format("Welcome email being processed for user %s", body.getEmail())));

        return emailService.sendEmail(emailService.buildWelcomeEmail(body, body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.MEDIA_NEW_ACCOUNT_SETUP.template)).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', forename: 'foo', surname: 'bar'}
     */
    public String azureNewUserEmailRequest(CreatedAdminWelcomeEmail body) {
        log.info(writeLog(String.format("New User Welcome email "
                                                   + "being processed for user %s", body.getEmail())));

        EmailToSend email = emailService.buildCreatedAdminWelcomeEmail(body,
                                                                       Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * Handles the incoming request for media applications reporting emails.
     * Creates a csv, builds and sends the email.
     *
     * @param mediaApplicationList The list of media applications to send in the email
     */
    public String handleMediaApplicationReportingRequest(List<MediaApplication> mediaApplicationList) {
        EmailToSend email = emailService.buildMediaApplicationReportingEmail(
            csvCreationService.createMediaApplicationReportingCsv(mediaApplicationList),
            Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template);

        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the subscription email, and forwarding on to the relevant email client.
     * @param body The subscription message that is to be fulfilled.
     * @return The ID that references the subscription message.
     */
    public String subscriptionEmailRequest(SubscriptionEmail body) {
        log.info(writeLog(String.format("Sending subscription email for user %s", body.getEmail())));

        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        if (artefact.getIsFlatFile()) {
            return emailService.sendEmail(emailService.buildFlatFileSubscriptionEmail(
                                                  body, artefact,
                                                  Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template))
                .getReference().orElse(null);
        } else {
            return emailService.sendEmail(emailService.buildRawDataSubscriptionEmail(
                body, artefact, Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.template))
                .getReference().orElse(null);
        }
    }

    /**
     * Handles the incoming request for duplicate media account emails,
     * checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', fullname: 'foo bar'}
     */
    public String mediaDuplicateUserEmailRequest(DuplicatedMediaEmail body) {
        EmailToSend email = emailService.buildDuplicateMediaSetupEmail(body,
                                                                     Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL.template);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the unidentified blobs email.
     *
     * @param locationMap A map of location Ids and provenances associated with unidentified blobs
     * @return The ID that references the unidentified blobs email.
     */
    public String unidentifiedBlobEmailRequest(Map<String, String> locationMap) {
        EmailToSend email = emailService
            .buildUnidentifiedBlobsEmail(locationMap, Templates.BAD_BLOB_EMAIL.template);

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for sending lists out to third party publishers, uses the artefact id from body
     * to retrieve Artefact from Data Management and then gets the file or json payload to then send out.
     * @param body Request body of ThirdParty subscription containing artefact id and the destination api.
     * @return String of successful POST.
     */
    public String handleThirdParty(ThirdPartySubscription body) {
        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        Location location = dataManagementService.getLocation(artefact.getLocationId());
        if (artefact.getIsFlatFile()) {
            log.info(thirdPartyService.handleThirdPartyCall(body.getApiDestination(),
                                                            dataManagementService.getArtefactFlatFile(
                                                                artefact.getArtefactId()), artefact, location));
        } else {
            log.info(thirdPartyService.handleThirdPartyCall(
                body.getApiDestination(),
                dataManagementService.getArtefactJsonBlob(
                    artefact.getArtefactId()), artefact, location
            ));
        }
        return String.format(SUCCESS_MESSAGE, body.getApiDestination());
    }

    public String handleThirdParty(String apiDestination) {

        log.info(writeLog("Sending blank payload to third party"));
        log.info(writeLog(thirdPartyService.handleThirdPartyCall(apiDestination, "", null, null)));

        return String.format(EMPTY_SUCCESS_MESSAGE, apiDestination);
    }
}
