package uk.gov.hmcts.reform.pip.publication.services.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.EmailHelper;

import java.time.Duration;

@Service
public class RateLimitingService {
    private static final String CACHE_PREFIX = "emailCache::";

    @Autowired
    private RedisTemplate<String, Integer> template;

    @Value("${notify.email.limit}")
    private int limit;

    public void validate(String email) {
        Integer count = getEmailCount(email);

        if (count == null) {
            initialiseEmailCount(email);
        } else if (count >= limit) {
            throw new TooManyEmailsException(String.format(
                "The number of emails sent to %s has exceeded the limit. Please try again later.",
                EmailHelper.maskEmail(email)
            ));
        }
        incrementEmailCount(email);
    }

    private void initialiseEmailCount(String email) {
        template.opsForValue()
            .setIfAbsent(CACHE_PREFIX + email, 0, Duration.ofMinutes(1));
    }

    private Integer getEmailCount(String email) {
        return template.opsForValue()
            .get(CACHE_PREFIX + email);
    }

    private void incrementEmailCount(String email) {
        template.opsForValue()
            .increment(CACHE_PREFIX + email);
    }
}
