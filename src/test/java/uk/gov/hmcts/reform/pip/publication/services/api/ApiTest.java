package uk.gov.hmcts.reform.pip.publication.services.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.EmailClientConfiguration;
import uk.gov.service.notify.SendEmailResponse;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EmailClientConfiguration.class, Application.class})
@TestPropertySource(locations = "classpath:application.yaml")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTest {
    private static final String EMAIL = "test@email.com";
    private static final String EXISTING_TEMPLATE_ID = "321cbaa6-2a19-4980-87c6-fe90516db59b";
    private static final String NEW_TEMPLATE_ID = "b708c2dc-5794-4468-a8bf-f798fe1f91bc";
    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\"}";
    private static final String VALID_WELCOME_REQUEST_BODY_NEW =
        "{\"email\": \"test@email.com\", \"isExisting\": \"false\"}";
    private static final String INVALID_JSON_BODY = "{\"email\": \"test@email.com\", \"isExisting\":}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private SendEmailResponse sendEmailResponse;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() throws Exception {
        when(emailClient.sendEmail(eq(EXISTING_TEMPLATE_ID), eq(EMAIL),
                                   any(), any())).thenReturn(sendEmailResponse);
        mockMvc.perform(post("/notify/welcome-email")
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testValidPayloadReturnsSuccessNew() throws Exception {
        when(emailClient.sendEmail(eq(NEW_TEMPLATE_ID), eq(EMAIL),
                                   any(), any())).thenReturn(sendEmailResponse);
        mockMvc.perform(post("/notify/welcome-email")
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/notify/welcome-email")
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
