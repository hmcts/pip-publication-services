package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

@Service
@Slf4j
public class NotificationService {

    private static final String EMAIL_KEY = "email";
    private static final String IS_EXISTING_KEY = "isExisting";

    @Autowired
    private EmailService emailService;

    /**
     * Handles the incoming request for welcome emails, checks the json payload and builds and sends the email.
     * @param body JSONObject containing the email and isExisting values e.g.
     *             {email: 'example@email.com', isExisting: 'true'}
     */
    public String handleWelcomeEmailRequest(JSONObject body) {
        validateRequiredBody(body, EMAIL_KEY, IS_EXISTING_KEY);

        boolean isExisting;
        try {
            isExisting = body.getBoolean(IS_EXISTING_KEY);
        } catch (JSONException e) {
            throw (BadPayloadException) new BadPayloadException(e.getMessage()).initCause(e);
        }
        String email = body.get(EMAIL_KEY).toString();

        return emailService.buildEmail(email, isExisting
            ? Templates.EXISTING_USER_WELCOME_EMAIL.template :
            Templates.NEW_USER_WELCOME_EMAIL.template).getReference().orElse(null);
    }

    private void validateRequiredBody(JSONObject body, String... keys) {
        for (String key : keys) {
            if (!body.has(key)) {
                throw new BadPayloadException(String.format("%s was not found in the json payload", key));
            }
        }
    }
}
