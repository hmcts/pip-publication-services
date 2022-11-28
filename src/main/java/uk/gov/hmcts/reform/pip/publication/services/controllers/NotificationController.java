package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication.services.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.request.AdminActionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.InactiveUserNotificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaVerificationEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.models.request.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

@RestController
@Tag(name = "Publication Services notification API")
@RequestMapping("/notify")
@IsAdmin
@SuppressWarnings("PMD.TooManyMethods")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    private static final String BAD_PAYLOAD_EXCEPTION_MESSAGE = "BadPayloadException error message";

    private static final String BAD_PAYLOAD_ERROR_MESSAGE = "BadPayloadException error message";
    private static final String NOTIFY_EXCEPTION_ERROR_MESSAGE = "NotifyException error message";

    private static final String OK_RESPONSE = "200";
    private static final String AUTH_RESPONSE = "403";
    private static final String BAD_REQUEST = "400";

    /**
     * api to send welcome emails to new or existing users.
     *
     * @param body must contain a recipient email address and an new/existing user bool: {email:
     *             example@email.com, isExisting: true}
     * @return HTTP status upon completion
     */
    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Welcome email successfully "
            + "sent with referenceId abc123-123-432-4456"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE),
        @ApiResponse(responseCode = AUTH_RESPONSE, description = "User has not been authorized"),
    })
    @Operation(summary = "Send welcome email to new or existing subscribed users",
        description = "Use the bool isExisting as 'false' to send new user emails or 'true' to "
            + "send existing user emails ")
    @PostMapping("/welcome-email")
    public ResponseEntity<String> sendWelcomeEmail(@RequestBody WelcomeEmail body) {
        return ResponseEntity.ok(String.format(
            "Welcome email successfully sent with referenceId %s",
            notificationService.handleWelcomeEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Created admin welcome email "
            + "successfully sent with referenceId {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send welcome email to new Azure Active Directory (AAD) user.")
    @PostMapping("/created/admin")
    public ResponseEntity<String> sendAdminAccountWelcomeEmail(@RequestBody CreatedAdminWelcomeEmail body) {
        return ResponseEntity.ok(String.format(
            "Created admin welcome email successfully sent with referenceId %s",
            notificationService.azureNewUserEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Media applications report "
            + "email sent successfully with referenceId {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = "CsvCreationException error message")
    })
    @Operation(summary = "Send the media application report to the P&I team")
    @PostMapping("/media/report")
    public ResponseEntity<String> sendMediaReportingEmail(@RequestBody List<MediaApplication> mediaApplicationList) {
        return ResponseEntity.ok(String.format(
            "Media applications report email sent successfully with referenceId %s",
                notificationService.handleMediaApplicationReportingRequest(
                    mediaApplicationList)));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description =
            "Subscription email successfully sent to email: {recipientEmail} with reference id: {reference id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send subscription email to user")
    @PostMapping("/subscription")
    public ResponseEntity<String> sendSubscriptionEmail(@Valid @RequestBody SubscriptionEmail body) {
        return ResponseEntity.ok(String.format(
            "Subscription email successfully sent to email: %s with reference id: %s", body.getEmail(),
            notificationService.subscriptionEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Unidentified blob email "
            + "successfully sent with referenceId: {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send the unidentified blob report to the P&I team")
    @PostMapping("/unidentified-blob")
    public ResponseEntity<String> sendUnidentifiedBlobEmail(@RequestBody Map<String, String> locationMap) {
        return ResponseEntity.ok(String.format(
            "Unidentified blob email successfully sent with reference id: %s",
            notificationService.unidentifiedBlobEmailRequest(locationMap)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Duplicate media account email "
            + "successfully sent with referenceId {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send duplicate email to new media account user.")
    @PostMapping("/duplicate/media")
    public ResponseEntity<String> sendDuplicateMediaAccountEmail(@RequestBody DuplicatedMediaEmail body) {
        return ResponseEntity.ok(String.format(
            "Duplicate media account email successfully sent with referenceId %s",
            notificationService.mediaDuplicateUserEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Successfully sent list to {thirdParty} at: {api}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_EXCEPTION_MESSAGE)
    })
    @Operation(summary = "Send list to third party publisher")
    @PostMapping("/api")
    public ResponseEntity<String> sendThirdPartySubscription(@Valid @RequestBody ThirdPartySubscription body) {
        return ResponseEntity.ok(notificationService.handleThirdParty(body));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Successfully sent empty "
            + "list to {thirdParty} at: {api}"),
    })
    @Operation(summary = "Send empty list to third party after being deleted from P&I")
    @PutMapping("/api")
    public ResponseEntity<String> sendThirdPartySubscription(@Valid @RequestBody ThirdPartySubscriptionArtefact body) {
        return ResponseEntity.ok(notificationService.handleThirdParty(body));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Media user verification email "
            + "successfully sent with referenceId: {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send a media user a verification email")
    @PostMapping("/media/verification")
    public ResponseEntity<String> sendMediaUserVerificationEmail(@RequestBody MediaVerificationEmail body) {
        return ResponseEntity.ok(String.format(
            "Media user verification email successfully sent with referenceId: %s",
            notificationService.mediaUserVerificationEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "Inactive user sign-in notification"
            + " email successfully sent with "
            + "referenceId: {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send notification email to inactive users to remind them to sign in")
    @PostMapping("/user/sign-in")
    public ResponseEntity<String> sendNotificationToInactiveUsers(@RequestBody InactiveUserNotificationEmail body) {
        return ResponseEntity.ok(String.format(
            "Inactive user sign-in notification email successfully sent with referenceId: %s",
            notificationService.inactiveUserNotificationEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "MI data reporting email successfully sent with "
            + "referenceId: {Id}"),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send email with MI report")
    @PostMapping("/mi/report")
    public ResponseEntity<String> sendMiReportingEmail() {
        return ResponseEntity.ok(String.format(
            "MI data reporting email successfully sent with referenceId: %s",
            notificationService.handleMiDataForReporting()
        ));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_RESPONSE, description = "System Admin user email notification"),
        @ApiResponse(responseCode = BAD_REQUEST, description = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(responseCode = BAD_REQUEST, description = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @Operation(summary = "Send notification email to system admin about update")
    @PostMapping("/sysadmin/update")
    public ResponseEntity<String> sendSystemAdminUpdate(@RequestBody AdminActionEmail body) {
        return ResponseEntity.ok(String.format(
            "Send notification email email successfully sent to all system admin with referenceId: %s",
            notificationService.sendSystemAdminUpdateEmailRequest(body)
        ));
    }
}
