package uk.gov.hmcts.reform.pip.publication.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the Web Client that is used in requests to external services.
 */
@Configuration
@Profile("!test & !integration")
public class WebClientConfiguration {

    @Bean
    @Profile("!dev")
    public WebClient webClient(ClientRegistrationRepository clientRegistrations,
                        OAuth2AuthorizedClientRepository authorizedClients) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrations, authorizedClients);
        oauth2.setDefaultClientRegistrationId("dataManagementApi");
        return WebClient.builder().apply(oauth2.oauth2Configuration()).build();
    }

    @Bean
    @Profile("dev")
    public WebClient webClientInsecure() {
        return WebClient.builder().build();
    }

}
