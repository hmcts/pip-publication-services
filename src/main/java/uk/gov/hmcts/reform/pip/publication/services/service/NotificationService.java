package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

@Service
@Slf4j
public class NotificationService {

    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s";

    @Autowired
    private EmailService emailService;

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
        return emailService.sendEmail(emailService.buildWelcomeEmail(body, body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.NEW_USER_WELCOME_EMAIL.template)).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', forename: 'foo', surname: 'bar'}
     */
    public String azureNewUserEmailRequest(CreatedAdminWelcomeEmail body) {
        EmailToSend email = emailService.buildCreatedAdminWelcomeEmail(body,
                                                                       Templates.ADMIN_ACCOUNT_CREATION_EMAIL.template);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the subscription email, and forwarding on to the relevant email client.
     * @param body The subscription message that is to be fulfilled.
     * @return The ID that references the subscription message.
     */
    public String subscriptionEmailRequest(SubscriptionEmail body) {
        Artefact artefact = dataManagementService.getArtefact(body.getArtefactId());
        if (artefact.getIsFlatFile()) {
            return emailService.sendEmail(emailService.buildFlatFileSubscriptionEmail(
                                                  body, artefact,
                                                  Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.template))
                .getReference().orElse(null);
        } else {
            //TODO: Update once JSON generation has been completed to call the Non-Flat-File email.
            throw new UnsupportedOperationException(
                "Subscription service does not currently support publications for JSON payloads");
        }
    }

    /**
     * Handles the incoming request for sending lists out to third party publishers, uses the artefact id from body
     * to retrieve Artefact from Data Management and then gets the file or json payload to then send out.
     * @param body Request body of ThirdParty subscription containing artefact id and the destination api.
     * @return String of successful POST.
     */
    public String handleThirdParty(ThirdPartySubscription body) {
        body.getArtefactIds().forEach(artefactId -> {
            Artefact artefact = dataManagementService.getArtefact(artefactId);
            if (artefact.getIsFlatFile()) {
                log.info(thirdPartyService.handleThirdPartyCall(body.getApiDestination(),
                                                                dataManagementService.getArtefactFlatFile(
                                                               artefact.getArtefactId())));
            } else {
                log.info(thirdPartyService.handleThirdPartyCall(body.getApiDestination(),
                                                                dataManagementService.getArtefactJsonBlob(
                                                               artefact.getArtefactId())));
            }
        });
        return String.format(SUCCESS_MESSAGE, body.getApiDestination());
    }
}
