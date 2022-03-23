package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.AadWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.service.notify.SendEmailResponse;

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
        EmailToSend welcomeEmail = emailService.buildWelcomeEmail(body);
        SendEmailResponse response = emailService.sendEmail(welcomeEmail);
        return response.getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', forename: 'foo', surname: 'bar'}
     */
    public String azureNewUserEmailRequest(AadWelcomeEmail body) {
        EmailToSend email = emailService.buildAadWelcomeEmail(body);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }
}
