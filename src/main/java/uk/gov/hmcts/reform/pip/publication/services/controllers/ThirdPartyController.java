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
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.service.ThirdPartySubscriptionService;

@RestController
@Tag(name = "Publication Services third-party API")
@RequestMapping("/third-party")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class ThirdPartyController {
    private final ThirdPartySubscriptionService thirdPartySubscriptionService;

    @Autowired
    public ThirdPartyController(ThirdPartySubscriptionService thirdPartySubscriptionService) {
        this.thirdPartySubscriptionService = thirdPartySubscriptionService;
    }

    @ApiResponse(responseCode = "200", description = "Successfully sent publications/notifications to third parties")
    @ApiResponse(responseCode = "400", description = "Invalid third-party subscription format")
    @ApiResponse(responseCode = "401", description = "Invalid access credential")
    @ApiResponse(responseCode = "403", description = "User has not been authorized")
    @Operation(summary = "Send publications/notifications to third parties")
    @PostMapping
    public ResponseEntity<String> sendThirdPartySubscription(@Valid @RequestBody ThirdPartySubscription body) {
        return ResponseEntity.ok(thirdPartySubscriptionService.sendThirdPartySubscription(body));
    }
}
