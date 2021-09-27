package uk.gov.hmcts.reform.pip.publication_services.controllers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.BadPayloadException;
import uk.gov.hmcts.reform.pip.publication_services.service.NotificationService;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationControllerTest {

    private static final String VALID_WELCOME_REQUEST_BODY = "{email: 'test@email.com', isExisting: 'true'}";
    private static final String INVALID_WELCOME_REQUEST_BODY = "{email: 'test@email.com', isExisting: 'test'}";
    private static final String INVALID_JSON_BODY = "{email: 'test@email.com', incorrect: }";

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        JSONObject validRequestBody = new JSONObject(VALID_WELCOME_REQUEST_BODY);
        JSONObject invalidRequestBody = new JSONObject(INVALID_WELCOME_REQUEST_BODY);


        when(notificationService.handleWelcomeEmailRequest(argThat((JSONObject body) -> body.toString().equals(
            validRequestBody.toString())))).thenReturn("successId");

        doThrow(BadPayloadException.class).when(notificationService).handleWelcomeEmailRequest(argThat((JSONObject body) -> body.toString().equals(
            invalidRequestBody.toString())));
    }

    @Test
    public void testValidBodyShouldReturnOkResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(VALID_WELCOME_REQUEST_BODY))
            .andExpect(status().isOk())
            .andExpect(content().string("Welcome email successfully sent with referenceId successId"));
    }

    @Test
    public void testInvalidJsonShouldReturnBadRequestResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(INVALID_JSON_BODY))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidBodyShouldReturnBadRequestResponse() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(INVALID_WELCOME_REQUEST_BODY))
            .andExpect(status().isBadRequest());
    }
}
