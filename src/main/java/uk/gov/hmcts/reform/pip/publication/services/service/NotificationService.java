package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.request.AadWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private EmailService emailService;

    /**
     * Handles the incoming request for welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and isExisting values e.g.
     *             {email: 'example@email.com', isExisting: 'true'}
     */
    public String handleWelcomeEmailRequest(WelcomeEmail body) {
        return emailService.buildEmail(body.getEmail(), body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.NEW_USER_WELCOME_EMAIL.template).getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email value e.g.
     *             {email: 'example@email.com'}
     */
    public String azureNewUserEmailRequest(AadWelcomeEmail body) {
        return emailService.buildEmail(body.getEmail(), Templates.NEW_AZURE_USER_WELCOME_EMAIL.template)
            .getReference().orElse(null);
    }
}
