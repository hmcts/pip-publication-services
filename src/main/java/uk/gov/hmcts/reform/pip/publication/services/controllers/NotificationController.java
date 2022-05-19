package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication.services.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;
import javax.validation.Valid;

@RestController
@Api(tags = "Publication Services notification API")
@RequestMapping("/notify")
@IsAdmin
public class NotificationController {

    @Autowired
    NotificationService notificationService;

    /**
     * api to send welcome emails to new or existing users.
     *
     * @param body must contain a recipient email address and an new/existing user bool: {email:
     *             example@email.com, isExisting: true}
     * @return HTTP status upon completion
     */
    @ApiResponses({
        @ApiResponse(code = 200, message = "Welcome email successfully sent with referenceId abc123-123-432-4456"),
        @ApiResponse(code = 400, message = "BadPayloadException error message"),
        @ApiResponse(code = 400, message = "NotifyException error message"),
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
        @ApiResponse(code = 400, message = "BadPayloadException error message"),
        @ApiResponse(code = 400, message = "NotifyException error message")
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
        @ApiResponse(code = 200, message =
            "Subscription email successfully sent to email: {recipientEmail} with reference id: {reference id}"),
        @ApiResponse(code = 400, message = "BadPayloadException error message"),
        @ApiResponse(code = 400, message = "NotifyException error message")
    })
    @ApiImplicitParam(name = "body", example = "{\n" +
        "    \"email\": \"a@b.com\",\n" +
        "    \"subscriptions\": {\n" +
        "        \"CASE_URN\": [\n" +
        "            \"123\",\n" +
        "            \"321\"\n" +
        "        ],\n" +
        "        \"CASE_NUMBER\": [\"5445\"],\n" +
        "        \"LOCATION_ID\": [\"1\",\"2\"]\n" +
        "    },\n" +
        "    \"artefactId\": <artefactId>\n" +
        "}")
    @ApiOperation("Send subscription email to user")
    @PostMapping("/subscription")
    public ResponseEntity<String> sendSubscriptionEmail(@RequestBody SubscriptionEmail body) {
        return ResponseEntity.ok(String.format(
            "Subscription email successfully sent to email: %s with reference id: %s", body.getEmail(),
            notificationService.subscriptionEmailRequest(body)));
    }
}
