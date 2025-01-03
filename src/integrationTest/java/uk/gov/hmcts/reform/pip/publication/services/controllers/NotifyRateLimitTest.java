package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import org.hamcrest.core.IsNull;
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
import uk.gov.hmcts.reform.pip.publication.services.models.request.OtpEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.io.IOException;

import static okhttp3.tls.internal.TlsUtil.localhost;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin", "APPROLE_api.request.b2c"})
@ActiveProfiles("integration-rate-limit")
class NotifyRateLimitTest extends RedisConfigurationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String OTP_EMAIL_URL = "/notify/otp";

    private static final String FULL_NAME = "test full name";
    private static final String TEST_EMAIL = "test-email@justice.gov.uk";
    private static final String TEST_EMAIL2 = "test-email2@justice.gov.uk";
    private static final String TEST_EMAIL3 = "test-email3@justice.gov.uk";
    private static final String TEST_EMAIL4 = "test-email4@justice.gov.uk";
    private static final String TEST_EMAIL5 = "test-email5@justice.gov.uk";
    private static final String TEST_EMAIL6 = "test-email6@justice.gov.uk";

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
    void testRateLimitWithStandardCapacity() throws Exception {
        WelcomeEmail welcomeEmail = new WelcomeEmail(TEST_EMAIL, false, FULL_NAME);
        String welcomeEmailContent = OBJECT_MAPPER.writeValueAsString(welcomeEmail);

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(welcomeEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(welcomeEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(containsString("Rate limit has been exceeded. "
                                                           + "New media account welcome email failed to be sent to")));
    }

    @Test
    void testRateLimitWithHighCapacity() throws Exception {
        OtpEmail otpEmail = new OtpEmail("12345", TEST_EMAIL2);
        String otpEmailContent = OBJECT_MAPPER.writeValueAsString(otpEmail);

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(containsString("Rate limit has been exceeded. "
                                                           + "B2C OTP email failed to be sent to")));
    }

    @Test
    void testRateLimitUsingDifferentEmails() throws Exception {
        WelcomeEmail welcomeEmail = new WelcomeEmail(TEST_EMAIL3, false, FULL_NAME);
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(welcomeEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        welcomeEmail = new WelcomeEmail(TEST_EMAIL4, false, FULL_NAME);
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(welcomeEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        welcomeEmail = new WelcomeEmail(TEST_EMAIL5, false, FULL_NAME);
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(welcomeEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testRateLimitUsingSameEmailButDifferentCapacity() throws Exception {
        WelcomeEmail welcomeEmail = new WelcomeEmail(TEST_EMAIL6, false, FULL_NAME);
        String welcomeEmailContent = OBJECT_MAPPER.writeValueAsString(welcomeEmail);

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(welcomeEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        OtpEmail otpEmail = new OtpEmail("12345", TEST_EMAIL6);
        String otpEmailContent = OBJECT_MAPPER.writeValueAsString(otpEmail);

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        mockMvc.perform(post(OTP_EMAIL_URL)
                            .content(otpEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests());

        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(welcomeEmailContent)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests());
    }
}
