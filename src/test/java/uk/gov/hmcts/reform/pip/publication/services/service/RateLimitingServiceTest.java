package uk.gov.hmcts.reform.pip.publication.services.service;

import io.github.bucket4j.Bucket;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pip.publication.services.config.RateLimitConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {
    private static final String TEST_EMAIL = "test1@rateLimit.com";

    private static final Templates TEMPLATE_WITH_STANDARD_EMAIL_LIMIT = Templates.EXISTING_USER_WELCOME_EMAIL;
    private static final String STANDARD_CAPACITY_ERROR_MESSAGE = "Rate limit has been exceeded. Existing media "
        + "account welcome email failed to be sent to t****@rateLimit.com";

    private static final String EXCEPTION_MESSAGE = "Exception should be thrown";
    private static final String NO_EXCEPTION_MESSAGE = "Exception should not be thrown";
    private static final String RESPONSE_TRUE_MESSAGE = "Method should return true";
    private static final String RESPONSE_FALSE_MESSAGE = "Method should return false";
    private static final String LOG_MESSAGE = "Error log does not match";

    private LogCaptor logCaptor = LogCaptor.forClass(RateLimitingService.class);

    @Mock
    private RateLimitConfiguration rateLimitConfiguration;

    @Mock
    private Bucket bucket;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(rateLimitConfiguration, "standardEmailCapacity", 1);
        ReflectionTestUtils.setField(rateLimitConfiguration, "highEmailCapacity", 2);
        ReflectionTestUtils.setField(rateLimitConfiguration, "rateLimitInterval", 1);
    }

    @Test
    void testValidateThrowsExceptionAfterReachingEmailLimit() {
        when(rateLimitConfiguration.resolveBucket(any(), any())).thenReturn(bucket);
        when(bucket.tryConsume(1))
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false);

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(STANDARD_CAPACITY_ERROR_MESSAGE);
    }

    @Test
    void testIsValidReturnsFalseAfterReachingEmailLimit() {

        when(rateLimitConfiguration.resolveBucket(any(), any())).thenReturn(bucket);
        when(bucket.tryConsume(1))
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false);

        assertThat(rateLimitingService.isValid(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(RESPONSE_TRUE_MESSAGE)
            .isTrue();

        assertThat(rateLimitingService.isValid(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(RESPONSE_TRUE_MESSAGE)
            .isTrue();

        assertThat(rateLimitingService.isValid(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(RESPONSE_FALSE_MESSAGE)
            .isFalse();

        assertThat(logCaptor.getErrorLogs())
            .as(LOG_MESSAGE)
            .isNotEmpty();

        assertThat(logCaptor.getErrorLogs().get(0))
            .as(LOG_MESSAGE)
            .contains(STANDARD_CAPACITY_ERROR_MESSAGE);
    }
}
