package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsB2C;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.UserNotificationService;

@RestController
@Tag(name = "Publication Services notification API for B2C")
@RequestMapping("/notify")
@IsB2C
@SecurityRequirement(name = "bearerAuth")
public class B2cNotificationController {
    private final UserNotificationService userNotificationService;

    @Autowired
    public B2cNotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @ApiResponse(responseCode = "200", description = "OTP email successfully sent with referenceId: {Id}")
    @ApiResponse(responseCode = "400", description = "NotifyException error message")
    @ApiResponse(responseCode = "401", description = "Invalid access credential")
    @ApiResponse(responseCode = "403", description = "User has not been authorized")
    @Operation(summary = "Send email containing B2C one-time password")
    @PostMapping("/otp")
    public ResponseEntity<String> sendOtpEmail(@Valid @RequestBody OtpEmail body) {
        return ResponseEntity.ok(userNotificationService.handleOtpEmailRequest(body));
    }
}
