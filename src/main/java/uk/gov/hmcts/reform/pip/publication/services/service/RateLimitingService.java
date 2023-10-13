package uk.gov.hmcts.reform.pip.publication.services.service;

import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.config.RateLimitConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;

@Service
public class RateLimitingService {
    @Autowired
    private RateLimitConfiguration rateLimitConfiguration;

    public void validate(String email) {
        Bucket bucket = rateLimitConfiguration.resolveBucket(email);

        if (!bucket.tryConsume(1)) {
            throw new TooManyEmailsException(String.format(
                "The number of emails sent to %s has exceeded the limit. Please try again later.",
                EmailHelper.maskEmail(email)
            ));
        }
    }
}
