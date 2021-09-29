package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.pip.publication.services.models.request.WelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.service.NotificationService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationControllerTest {

    private static final String VALID_EMAIL = "test@email.com";
    private static final boolean TRUE_BOOL = true;

    private static final String VALID_WELCOME_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"isExisting\": \"true\"}";
    private static final String INVALID_WELCOME_REQUEST_BODY =
        "{\"email\": \"test@email.com\", \"isExisting\": \"test\"}";
    private static final String INVALID_REQUEST_BODY = "{\"email\": \"test@email.com\", \"isExisting\"}";

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        WelcomeEmail validRequestBodyTrue = new WelcomeEmail(VALID_EMAIL, TRUE_BOOL);

        when(notificationService.handleWelcomeEmailRequest(validRequestBodyTrue)).thenReturn("successId");
    }

    @Test
    public void testValidBodyShouldReturnOkResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email")
                            .content(VALID_WELCOME_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Welcome email successfully sent with referenceId successId"));
    }

    @Test
    public void testInvalidRequestShouldReturnBadRequestResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email")
                            .content(INVALID_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidTypeShouldReturnBadRequestResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email")
                            .content(INVALID_WELCOME_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
