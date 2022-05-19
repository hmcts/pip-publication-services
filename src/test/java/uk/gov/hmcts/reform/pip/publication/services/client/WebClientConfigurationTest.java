package uk.gov.hmcts.reform.pip.publication.services.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("test")
public class WebClientConfigurationTest {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

}
