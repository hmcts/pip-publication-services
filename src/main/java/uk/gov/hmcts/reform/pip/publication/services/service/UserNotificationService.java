package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaRejectionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class UserNotificationService {

    private final EmailService emailService;

    @Autowired
    public UserNotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Handles the incoming request for welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and isExisting values e.g.
     *             {email: 'example@email.com', isExisting: 'true'}
     */
    public String handleWelcomeEmailRequest(WelcomeEmail body) {
        log.info(writeLog(String.format("Media account welcome email being processed for user %s",
            EmailHelper.maskEmail(body.getEmail()))));

        return emailService.sendEmail(emailService.buildWelcomeEmail(body, body.isExisting()
            ? Templates.EXISTING_USER_WELCOME_EMAIL : Templates.MEDIA_NEW_ACCOUNT_SETUP))
            .getReference().orElse(null);
    }

    /**
     * Handles the incoming request for AAD welcome emails, checks the json payload and builds and sends the email.
     *
     * @param body JSONObject containing the email and forename/surname values e.g.
     *             {email: 'example@email.com', forename: 'foo', surname: 'bar'}
     */
    public String azureNewUserEmailRequest(CreatedAdminWelcomeEmail body) {
        log.info(writeLog(String.format("Admin account welcome email being processed for user %s",
            EmailHelper.maskEmail(body.getEmail()))));

        EmailToSend email = emailService.buildCreatedAdminWelcomeEmail(body,
            Templates.ADMIN_ACCOUNT_CREATION_EMAIL);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
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
            Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL);
        return emailService.sendEmail(email)
            .getReference().orElse(null);
    }

    /**
     * This method handles the sending of the media user verification email.
     *
     * @param body The body of the media verification email.
     * @return The ID that references the media user verification email.
     */
    public String mediaUserVerificationEmailRequest(MediaVerificationEmail body) {
        EmailToSend email = emailService
            .buildMediaUserVerificationEmail(body, Templates.MEDIA_USER_VERIFICATION_EMAIL);

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * This method handles the sending of the media user rejection email.
     *
     * @param body The body of the media rejection email.
     * @return The ID that references the media user rejection email.
     */
    public String mediaUserRejectionEmailRequest(MediaRejectionEmail body) {
        EmailToSend email = emailService
            .buildMediaApplicationRejectionEmail(body, Templates.MEDIA_USER_REJECTION_EMAIL);

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    /**
     * Handles the sending of the inactive user notification email.
     *
     * @param body The body of the inactive user notification email.
     * @return The ID that references the inactive user notification email.
     */
    public String inactiveUserNotificationEmailRequest(InactiveUserNotificationEmail body) {
        EmailToSend email;
        if ("PI_AAD".equals(body.getUserProvenance())) {
            email = emailService
                .buildInactiveUserNotificationEmail(body, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD);
        } else {
            email = emailService
                .buildInactiveUserNotificationEmail(body, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT);
        }

        return emailService.sendEmail(email).getReference().orElse(null);
    }

    public String handleOtpEmailRequest(OtpEmail body) {
        log.info(writeLog(String.format("OTP email being processed for user %s",
                                        EmailHelper.maskEmail(body.getEmail()))));

        EmailToSend email = emailService.buildOtpEmail(body.getEmail(), body.getOtp(), Templates.OTP_EMAIL);
        return emailService.sendEmail(email)
            .getReference()
            .orElse(null);
    }
}
