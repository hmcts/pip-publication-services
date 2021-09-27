package uk.gov.hmcts.reform.pip.publication.services.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
@AutoConfigureMockMvc
public class IntegrationTest {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING = "{email: 'test@email.com', isExisting: 'true'}";
    private static final String VALID_WELCOME_REQUEST_BODY_NEW = "{email: 'test@email.com', isExisting: 'false'}";
    private static final String INVALID_JSON_BODY = "{email: 'test@email.com', incorrect: }";


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testValidPayloadReturnsSuccessExisting() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(VALID_WELCOME_REQUEST_BODY_EXISTING))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    public void testValidPayloadReturnsSuccessNew() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(VALID_WELCOME_REQUEST_BODY_NEW))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome email successfully sent with referenceId")));
    }

    @Test
    public void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/notify/welcome-email").content(INVALID_JSON_BODY))
            .andExpect(status().isBadRequest());
    }
}
