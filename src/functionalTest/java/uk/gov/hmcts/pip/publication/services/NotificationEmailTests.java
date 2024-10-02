package uk.gov.hmcts.pip.publication.services;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.pip.publication.services.utils.FunctionalTestBase;
import uk.gov.hmcts.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;

import java.util.Map;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, OAuthClient.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles(profiles = "functional")
class NotificationEmailTests extends FunctionalTestBase {
    private static final String BEARER = "Bearer ";

    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_WELCOME_EMAIL_URL = NOTIFY_URL + "/welcome-email";

    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_FULL_NAME = "test user";

    @Test
    void shouldSendMediaWelcomeEmailForNewUser() {
        WelcomeEmail requestBody = new WelcomeEmail(TEST_EMAIL, false, TEST_FULL_NAME);

        final Response response = doPostRequest(MEDIA_WELCOME_EMAIL_URL, Map.of(AUTHORIZATION, BEARER + accessToken),
                                                requestBody);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }
}
