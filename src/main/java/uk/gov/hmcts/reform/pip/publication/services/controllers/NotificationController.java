package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication.services.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;
import uk.gov.hmcts.reform.pip.publication.services.service.PdfCreationService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;

@RestController
@Api(tags = "Publication Services notification API")
@RequestMapping("/notify")
@IsAdmin
public class NotificationController {

    @Autowired
    NotificationService notificationService;

    @Autowired
    PdfCreationService pdfCreationService;


    private static final String BAD_PAYLOAD_ERROR_MESSAGE = "BadPayloadException error message";
    private static final String NOTIFY_EXCEPTION_ERROR_MESSAGE = "NotifyException error message";

    /**
     * api to send welcome emails to new or existing users.
     *
     * @param body must contain a recipient email address and an new/existing user bool: {email:
     *             example@email.com, isExisting: true}
     * @return HTTP status upon completion
     */
    @ApiResponses({
        @ApiResponse(code = 200, message = "Welcome email successfully sent with referenceId abc123-123-432-4456"),
        @ApiResponse(code = 400, message = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(code = 400, message = NOTIFY_EXCEPTION_ERROR_MESSAGE),
        @ApiResponse(code = 403, message = "User has not been authorized"),
    })
    @ApiOperation(value = "Send welcome email to new or existing subscribed users",
        notes = "Use the bool isExisting as 'false' to send new user emails or 'true' to send existing user emails ")
    @ApiImplicitParam(name = "body", example = "{\n email: 'example@email.com',\n isExisting: 'true'\n}")
    @PostMapping("/welcome-email")
    public ResponseEntity<String> sendWelcomeEmail(@RequestBody WelcomeEmail body) {
        return ResponseEntity.ok(String.format(
            "Welcome email successfully sent with referenceId %s",
            notificationService.handleWelcomeEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Created admin welcome email successfully sent with referenceId {Id}"),
        @ApiResponse(code = 400, message = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(code = 400, message = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @ApiOperation("Send welcome email to new Azure Active Directory (AAD) user.")
    @ApiImplicitParam(name = "body", example = "{\n email: 'example@email.com',"
        + "\n forename: 'forename', \n"
        + "surname: 'surname' \n}")
    @PostMapping("/created/admin")
    public ResponseEntity<String> sendAdminAccountWelcomeEmail(@RequestBody CreatedAdminWelcomeEmail body) {
        return ResponseEntity.ok(String.format(
            "Created admin welcome email successfully sent with referenceId %s",
            notificationService.azureNewUserEmailRequest(body)
        ));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Media applications report email sent successfully with referenceId {Id}"),
        @ApiResponse(code = 400, message = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(code = 400, message = NOTIFY_EXCEPTION_ERROR_MESSAGE),
        @ApiResponse(code = 400, message = "CsvCreationException error message")
    })
    @ApiOperation("Send the media application report to the P&I team")
    @PostMapping("/media/report")
    public ResponseEntity<String> sendMediaReportingEmail(@RequestBody List<MediaApplication> mediaApplicationList) {
        return ResponseEntity.ok(String.format(
            "Media applications report email sent successfully with referenceId %s",
                notificationService.handleMediaApplicationReportingRequest(
                    mediaApplicationList)));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message =
            "Subscription email successfully sent to email: {recipientEmail} with reference id: {reference id}"),
        @ApiResponse(code = 400, message = BAD_PAYLOAD_ERROR_MESSAGE),
        @ApiResponse(code = 400, message = NOTIFY_EXCEPTION_ERROR_MESSAGE)
    })
    @ApiImplicitParam(name = "body", example = "{\n"
        + "    \"email\": \"a@b.com\",\n"
        + "    \"subscriptions\": {\n"
        + "        \"CASE_URN\": [\n"
        + "            \"123\",\n"
        + "            \"321\"\n"
        + "        ],\n"
        + "        \"CASE_NUMBER\": [\"5445\"],\n"
        + "        \"LOCATION_ID\": [\"1\",\"2\"]\n"
        + "    },\n"
        + "    \"artefactId\": <artefactId>\n"
        + "}")
    @ApiOperation("Send subscription email to user")
    @PostMapping("/subscription")
    public ResponseEntity<String> sendSubscriptionEmail(@Valid @RequestBody SubscriptionEmail body) {
        return ResponseEntity.ok(String.format(
            "Subscription email successfully sent to email: %s with reference id: %s", body.getEmail(),
            notificationService.subscriptionEmailRequest(body)));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "artefactId received"),
    })
    @ApiOperation("Create PDF from blob")
    @PostMapping("/createPDF")
    public ResponseEntity<byte[]> createPdf(@RequestParam UUID artefactId) throws IOException {
        byte[] returnedHtml = pdfCreationService.jsonToPdf(artefactId);
        return ResponseEntity.ok(returnedHtml);
    }
}
