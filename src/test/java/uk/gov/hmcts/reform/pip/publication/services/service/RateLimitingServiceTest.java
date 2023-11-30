package uk.gov.hmcts.reform.pip.publication.services.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@DirtiesContext
@ActiveProfiles("test")
class RateLimitingServiceTest extends RedisConfigurationTestBase {
    private static final String TEST_EMAIL = "test1@rateLimit.com";
    private static final String TEST_EMAIL2 = "test2@rateLimit.com";
    private static final String TEST_EMAIL3 = "test3@rateLimit.com";
    private static final String TEST_EMAIL4 = "test4@rateLimit.com";
    private static final String TEST_EMAIL5 = "test5@rateLimit.com";
    private static final String TEST_EMAIL6 = "test6@rateLimit.com";

    private static final Templates TEMPLATE_WITH_STANDARD_EMAIL_LIMIT = Templates.EXISTING_USER_WELCOME_EMAIL;
    private static final Templates TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT = Templates.SYSTEM_ADMIN_UPDATE_EMAIL;
    private static final String STANDARD_CAPACITY_ERROR_MESSAGE = "Rate limit has been exceeded. Existing media "
        + "account welcome email failed to be sent to t****@rateLimit.com";
    private static final String HIGH_CAPACITY_ERROR_MESSAGE = "Rate limit has been exceeded. System admin "
        + "notification email failed to be sent to t****@rateLimit.com";

    private static final String EXCEPTION_MESSAGE = "Exception should be thrown";
    private static final String NO_EXCEPTION_MESSAGE = "Exception should not be thrown";
    private static final String RESPONSE_TRUE_MESSAGE = "Method should return true";
    private static final String RESPONSE_FALSE_MESSAGE = "Method should return false";
    private static final String LOG_MESSAGE = "Error log does not match";

    private LogCaptor logCaptor = LogCaptor.forClass(RateLimitingService.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Test
    void testValidateThrowsExceptionAfterReachingStandardEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(STANDARD_CAPACITY_ERROR_MESSAGE);
    }

    @Test
    void testValidateThrowsExceptionAfterReachingHighCapacityEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(HIGH_CAPACITY_ERROR_MESSAGE);
    }

    @Test
    void testValidateThrowsExceptionUsingTheSameLimitIfEmailsDifferent() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL2, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL3, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();
    }

    @Test
    void testEmailLimitsAppliedIndependentlyUsingTheSameEmail() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(STANDARD_CAPACITY_ERROR_MESSAGE);

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(HIGH_CAPACITY_ERROR_MESSAGE);
    }

    @Test
    void testIsValidReturnsFalseAfterReachingStandardEmailLimit() {
        assertThat(rateLimitingService.isValid(TEST_EMAIL5, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(RESPONSE_TRUE_MESSAGE)
            .isTrue();

        assertThat(rateLimitingService.isValid(TEST_EMAIL5, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(RESPONSE_FALSE_MESSAGE)
            .isFalse();

        assertThat(logCaptor.getErrorLogs())
            .as(LOG_MESSAGE)
            .isNotEmpty();

        assertThat(logCaptor.getErrorLogs().get(0))
            .as(LOG_MESSAGE)
            .contains(STANDARD_CAPACITY_ERROR_MESSAGE);
    }

    @Test
    void testIsValidReturnsFalseAfterReachingHighCapacityEmailLimit() {
        assertThat(rateLimitingService.isValid(TEST_EMAIL6, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(RESPONSE_TRUE_MESSAGE)
            .isTrue();

        assertThat(rateLimitingService.isValid(TEST_EMAIL6, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(RESPONSE_TRUE_MESSAGE)
            .isTrue();

        assertThat(rateLimitingService.isValid(TEST_EMAIL6, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(RESPONSE_FALSE_MESSAGE)
            .isFalse();

        assertThat(logCaptor.getErrorLogs())
            .as(LOG_MESSAGE)
            .isNotEmpty();

        assertThat(logCaptor.getErrorLogs().get(0))
            .as(LOG_MESSAGE)
            .contains(HIGH_CAPACITY_ERROR_MESSAGE);
    }
}
