package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.TooManyEmailsException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@ActiveProfiles("test")
class RateLimitingServiceTest {
    private static final String TEST_EMAIL = "test1@rateLimit.com";
    private static final String TEST_EMAIL2 = "test2@rateLimit.com";
    private static final String TEST_EMAIL3 = "test3@rateLimit.com";
    private static final String TEST_EMAIL4 = "test4@rateLimit.com";

    private static final String ERROR_MESSAGE = "Email failed to be sent to t****@rateLimit.com as the rate limit "
        + "has been exceeded for the user";
    private static final String EXCEPTION_MESSAGE = "Exception should be thrown";
    private static final String NO_EXCEPTION_MESSAGE = "Exception should not be thrown";

    @Autowired
    private RateLimitingService rateLimitingService;

    @Test
    void testExceptionIsThrownAfterReachingStandardEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, EmailLimit.STANDARD))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, EmailLimit.STANDARD))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    void testExceptionIsThrownAfterReachingHighCapacityEmailLimit() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, EmailLimit.HIGH))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL, EmailLimit.HIGH))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL, EmailLimit.HIGH))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    void testExceptionNotThrownUsingTheSameLimitIfEmailsDifferent() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL2, EmailLimit.STANDARD))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL3, EmailLimit.STANDARD))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();
    }

    @Test
    void testEmailLimitsAppliedIndependentlyUsingTheSameEmail() {
        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, EmailLimit.HIGH))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, EmailLimit.STANDARD))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL4, EmailLimit.STANDARD))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);

        assertThatCode(() -> rateLimitingService.validate(TEST_EMAIL4, EmailLimit.HIGH))
            .as(NO_EXCEPTION_MESSAGE)
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimitingService.validate(TEST_EMAIL4, EmailLimit.HIGH))
            .as(EXCEPTION_MESSAGE)
            .isInstanceOf(TooManyEmailsException.class)
            .hasMessage(ERROR_MESSAGE);
    }
}
