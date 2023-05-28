package uk.gov.hmcts.reform.pip.publication.services.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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
    // Currently we allow a maximum 2MB of PDF/Excel file to be transferred from Channel-management (same as GOV.UK
    // Notify file size constraint). The file content is sent as a Base64 encoded string which add roughly 33% space
    // overhead. Hence we increase the service-to-service size constraint to 3MB.
    private static final ExchangeStrategies STRATEGIES =  ExchangeStrategies.builder()
        .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
            .maxInMemorySize(3 * 1024 * 1024))
        .build();

    @Value("${third-party.certificate}")
    private String trustStore;

    @Bean
    @Profile("!dev")
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clients) {
        OAuth2AuthorizedClientService service = new InMemoryOAuth2AuthorizedClientService(clients);
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service);

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        manager.setAuthorizedClientProvider(authorizedClientProvider);

        return manager;
    }

    @Bean
    @Profile("!dev")
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId("dataManagementApi");
        return WebClient.builder().exchangeStrategies(STRATEGIES)
            .apply(oauth2Client.oauth2Configuration()).build();
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
        return WebClient.builder()
            .exchangeStrategies(STRATEGIES)
            .build();
    }
}
