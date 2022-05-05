package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.publication.services.Application;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
class NotifyTest {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\"}";
    private static final String VALID_WELCOME_REQUEST_BODY_NEW =
        "{\"email\": \"test@email.com\", \"isExisting\": \"false\"}";
    private static final String VALID_ADMIN_CREATION_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"surname\": \"surname\", \"forename\": \"forename\"}";
    private static final String INVALID_JSON_BODY = "{\"email\": \"test@email.com\", \"isExisting\":}";
    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testValidPayloadReturnsSuccessExisting() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testValidPayloadReturnsSuccessNew() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadReturnsSuccessAdminCreation() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "Created admin welcome email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequestAdminCreation() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "unknown_user", authorities = { "APPROLE_api.request.unknown" })
    void testUnauthorizedRequestWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "unknown_user", authorities = { "APPROLE_api.request.unknown" })
    void testUnauthorizedRequestAdminEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
