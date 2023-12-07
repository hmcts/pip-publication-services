package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationFunctionalTestBase;

import java.io.IOException;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("functional-rate-limit")
class NotifyRateLimitTest extends RedisConfigurationFunctionalTestBase {
    private static final String NOTIFY_SYSTEM_ADMIN_URL = "/notify/sysadmin/update";
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String RANDOM_EMAIL = "test"
        + RandomStringUtils.randomAlphanumeric(5) + "@justice.gov.uk";
    private static final String RANDOM_EMAIL_SYSTEM_ADMIN = "test.sa"
        + RandomStringUtils.randomAlphanumeric(5) + "@justice.gov.uk";

    private static final String VALID_WELCOME_REQUEST_BODY_NEW = "{\"email\": \""
        + RANDOM_EMAIL + "\", \"isExisting\": \"false\", \"fullName\": \"fullName\"}";

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY = "{\"requesterName\": \"reqName\","
        + " \"actionResult\": \"ATTEMPTED\",\"changeType\": \"DELETE_LOCATION\", \"emailList\": "
        + "[\"" + RANDOM_EMAIL_SYSTEM_ADMIN + "\"],\"detailString\": \"test\"}";

    private MockWebServer externalApiMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        HandshakeCertificates handshakeCertificates = localhost();
        externalApiMockServer = new MockWebServer();
        externalApiMockServer.useHttps(handshakeCertificates.sslSocketFactory(), false);
        externalApiMockServer.start(4444);
    }

    @AfterEach
    void tearDown() throws IOException {
        externalApiMockServer.close();
    }

    @Test
    void testRateLimitStandardWelcomeRequestNew() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(containsString("Rate limit has been exceeded. "
                                                           + "New media account welcome email failed to be sent to")));
    }

    @Test
    void testRateLimitHighSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Send notification email successfully to all system admin with referenceId")));
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString(
                "Send notification email successfully to all system admin with referenceId: []"))));
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Send notification email successfully to all system admin with referenceId: []")));
    }
}
