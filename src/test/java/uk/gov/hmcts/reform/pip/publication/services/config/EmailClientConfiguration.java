package uk.gov.hmcts.reform.pip.publication.services.config;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.service.notify.SendEmailResponse;

@Configuration
@Profile("test")
public class EmailClientConfiguration {

    @Mock
    EmailClient mockEmailClient;

    @Mock
    SendEmailResponse mockSendEmailResponse;

    public EmailClientConfiguration() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    @Primary
    public EmailClient emailClient() {
        return mockEmailClient;
    }

    @Bean
    public SendEmailResponse sendEmailResponse() {
        return mockSendEmailResponse;
    }

}
