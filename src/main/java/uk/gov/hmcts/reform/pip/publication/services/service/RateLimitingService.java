package uk.gov.hmcts.reform.pip.publication.services.service;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.config.RateLimitConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class RateLimitingService {
    private static final String EMAIL_PREFIX_SEPARATOR = "::";
    private static final String ERROR_MESSAGE = "Rate limit has been exceeded. %s failed to be sent to %s";

    @Autowired
    private RateLimitConfiguration rateLimitConfiguration;

    public void validate(String email, Templates emailTemplate) {
        if (!isEmailWithinLimit(email, emailTemplate.getEmailLimit())) {
            throw new TooManyEmailsException(getErrorMessage(email, emailTemplate.getDescription()));
        }
    }

    public boolean isValid(String email, Templates emailTemplate) {
        boolean isValid = isEmailWithinLimit(email, emailTemplate.getEmailLimit());
        if (!isValid) {
            log.error(writeLog(getErrorMessage(email, emailTemplate.getDescription())));
        }
        return isValid;
    }

    private boolean isEmailWithinLimit(String email, EmailLimit emailLimit) {
        String keyPrefix = emailLimit.getPrefix() + EMAIL_PREFIX_SEPARATOR;
        Bucket bucket = rateLimitConfiguration.resolveBucket(keyPrefix + email, emailLimit);
        return bucket.tryConsume(1);
    }

    private String getErrorMessage(String email, String emailescription) {
        return String.format(ERROR_MESSAGE, emailescription, EmailHelper.maskEmail(email));
    }
}
