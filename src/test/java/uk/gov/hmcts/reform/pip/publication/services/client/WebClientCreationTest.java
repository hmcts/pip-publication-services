package uk.gov.hmcts.reform.pip.publication.services.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.publication.services.config.WebClientConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WebClientCreationTest {

    @Mock
    ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    OAuth2AuthorizedClientRepository clientRepository;

    @Test
    void createWebClient() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClient(clientRegistrationRepository, clientRepository);

        assertNotNull(webClient, "WebClient has not been created successfully");
    }

    @Test
    void createWebClientInsecure() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClientInsecure();

        assertNotNull(webClient, "WebClient has not been created successfully");
    }
}
