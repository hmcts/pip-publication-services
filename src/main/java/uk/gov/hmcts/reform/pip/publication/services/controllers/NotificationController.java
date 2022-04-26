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
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;

@RestController
@Api(tags = "Publication Services notification API")
@RequestMapping("/notify")
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
        @ApiResponse(code = 400, message = "NotifyException error message")
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
}
