package uk.gov.hmcts.reform.pip.publication.services.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.service.AccountManagementService;
import uk.gov.hmcts.reform.pip.publication.services.service.DataManagementService;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
public class IntegrationTestBase extends RedisConfigurationTestBase {

    private static String emailResponseValue;

    @MockitoBean
    protected AccountManagementService accountManagementService;

    @MockitoBean
    protected DataManagementService dataManagementService;

    @MockitoBean
    EmailClient emailClient;

    @BeforeAll
    static void setupAll() throws IOException {
        try (InputStream jsonInput = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("emailResponse.json")) {
            emailResponseValue = new String(jsonInput.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setupEmailClient() throws NotificationClientException {
        SendEmailResponse emailResponse = new SendEmailResponse(emailResponseValue);
        when(emailClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(emailResponse);
    }
}
