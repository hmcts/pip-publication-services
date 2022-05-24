package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private CsvCreationService csvCreationService;

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
}
