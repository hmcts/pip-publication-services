package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private DataManagementService dataManagementService;

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
     * This method handles the sending of the unidentified blobs email.
     *
     * @param locationIds A list of location Ids associated with unidentified blobs
     * @return The ID that references the unidentified blobs email.
     */
    public String unidentifiedBlobEmailRequest(List<Integer> locationIds) {
        EmailToSend email = emailService.buildUnidentifiedBlobsEmail(locationIds, Templates.BAD_BLOB_EMAIL.template);

        return emailService.sendEmail(email).getReference().orElse(null);
    }
}
