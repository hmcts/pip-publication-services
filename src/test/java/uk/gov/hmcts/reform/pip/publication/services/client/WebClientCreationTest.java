package uk.gov.hmcts.reform.pip.publication.services.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.publication.services.config.WebClientConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WebClientCreationTest {

    @Mock
    OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void createWebClient() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClient(authorizedClientManager);

        assertNotNull(webClient, "WebClient has not been created successfully");
    }

    @Test
    void createAuthorizedClientManager() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        OAuth2AuthorizedClientManager clientManager =
            webClientConfiguration.authorizedClientManager(clientRegistrationRepository);

        assertNotNull(clientManager,
                      "Client Manager has not been successfully created");
    }

    @Test
    void createWebClientInsecure() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClientInsecure();
        assertNotNull(webClient, "WebClient has not been created successfully");
    }
}
