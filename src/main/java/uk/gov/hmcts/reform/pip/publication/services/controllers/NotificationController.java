package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.annotations.Api;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
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
    @PostMapping("/welcome-email")
    public ResponseEntity sendWelcomeEmail(@RequestBody String body) {
        JSONObject request;
        try {
            request = new JSONObject(body);
        } catch (JSONException e) {
            throw (BadPayloadException)new BadPayloadException("Invalid JSON body: " + e.getMessage()).initCause(e);
        }
        return ResponseEntity.ok(String.format(
            "Welcome email successfully sent with referenceId %s",
            notificationService.handleWelcomeEmailRequest(request)
        ));
    }
}
