package uk.gov.hmcts.reform.pip.publication.services.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.net.ssl.SSLException;

/**
 * Configures the Web Client that is used in requests to external services.
 */
@Configuration
@Profile("!test & !functional")
public class WebClientConfiguration {

    @Value("${third-party.certificate}")
    private String trustStore;

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
    @Profile("!dev")
    public WebClient.Builder webClientBuilder() throws SSLException {
        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(new ByteArrayInputStream(Base64.getDecoder().decode(trustStore)))
            .build();

        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    @Profile("dev")
    public WebClient webClientInsecure() {
        return WebClient.builder().build();
    }

}
