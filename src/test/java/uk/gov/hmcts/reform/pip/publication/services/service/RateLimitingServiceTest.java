package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.RedisTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class, RedisTestConfiguration.class})
@DirtiesContext
@ActiveProfiles("test")
class RateLimitingServiceTest {
    private static final String TEST_EMAIL = "test1@rateLimit.com";
    private static final String TEST_EMAIL2 = "test2@rateLimit.com";
    private static final String TEST_EMAIL3 = "test3@rateLimit.com";
    private static final String TEST_EMAIL4 = "test4@rateLimit.com";

    private static final Templates TEMPLATE_WITH_STANDARD_EMAIL_LIMIT = Templates.EXISTING_USER_WELCOME_EMAIL;
    private static final Templates TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT = Templates.SYSTEM_ADMIN_UPDATE_EMAIL;

    private static final String ERROR_MESSAGE = "Rate limit has been exceeded.";
    private static final String EXCEPTION_MESSAGE = "Exception should be thrown";
    private static final String NO_EXCEPTION_MESSAGE = "Exception should not be thrown";

    @Autowired
    private RateLimitingService rateLimitingService;

    @Test
    void testExceptionIsThrownAfterReachingStandardEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_STANDARD_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    void testExceptionIsThrownAfterReachingHighCapacityEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    void testExceptionNotThrownUsingTheSameLimitIfEmailsDifferent() {
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
            .hasMessageContaining(ERROR_MESSAGE);

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL4, TEMPLATE_WITH_HIGH_CAPACITY_EMAIL_LIMIT))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessageContaining(ERROR_MESSAGE);
    }
}
