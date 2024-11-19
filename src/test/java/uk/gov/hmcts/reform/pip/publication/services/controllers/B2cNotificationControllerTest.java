package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.UserNotificationService;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class B2cNotificationControllerTest extends RedisConfigurationTestBase {
    private static final String VALID_EMAIL = "test@b2c.com";
    private static final String OTP = "123456";
    private static final String REFERENCE_ID = "999";
    private static final OtpEmail OTP_EMAIL = new OtpEmail(OTP, VALID_EMAIL);

    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private B2cNotificationController notificationController;

    @Test
    void testSendOtpEmailShouldReturnOkResponse() {
        when(userNotificationService.handleOtpEmailRequest(OTP_EMAIL)).thenReturn(REFERENCE_ID);
        ResponseEntity<String> response = notificationController.sendOtpEmail(OTP_EMAIL);

        assertThat(response.getStatusCode())
            .as("Status code does not match")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body does not match")
            .isEqualTo(REFERENCE_ID);
    }
}
