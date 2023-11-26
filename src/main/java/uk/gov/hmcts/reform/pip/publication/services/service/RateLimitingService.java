package uk.gov.hmcts.reform.pip.publication.services.service;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.config.RateLimitConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class RateLimitingService {
    private static final String EMAIL_PREFIX_SEPARATOR = "::";
    private static final String ERROR_MESSAGE = "Email failed to be sent to %s as the rate limit has been exceeded "
        + "for the user";

    @Autowired
    private RateLimitConfiguration rateLimitConfiguration;

    public void validate(String email, EmailLimit emailLimit) {
        if (!isEmailWithinLimit(email, emailLimit)) {
            throw new TooManyEmailsException(getErrorMessage(email));
        }
    }

    public boolean isValid(String email, EmailLimit emailLimit) {
        boolean isValid = isEmailWithinLimit(email, emailLimit);
        if (!isValid) {
            log.error(writeLog(getErrorMessage(email)));
        }
        return isValid;
    }

    private boolean isEmailWithinLimit(String email, EmailLimit emailLimit) {
        String keyPrefix = emailLimit.getPrefix() + EMAIL_PREFIX_SEPARATOR;
        Bucket bucket = rateLimitConfiguration.resolveBucket(keyPrefix + email, emailLimit);
        return bucket.tryConsume(1);
    }

    private String getErrorMessage(String email) {
        return String.format(ERROR_MESSAGE, EmailHelper.maskEmail(email));
    }
}
