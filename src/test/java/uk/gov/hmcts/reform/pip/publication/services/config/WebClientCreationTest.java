package uk.gov.hmcts.reform.pip.publication.services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.ClientAttributes;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientCreationTest {
    private static final URI TEST_URI = URI.create("https://example.com");

    @Mock
    OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    OAuth2AuthorizedClient authorizedClient;

    @Test
    void createWebClient() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClient(authorizedClientManager);

        assertNotNull(webClient, "WebClient has not been created successfully");
    }

    @Test
    void createWebClientInsecure() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClientInsecure();

        assertNotNull(webClient, "WebClient has not been created successfully");
    }

    @Test
    void withBearerTokenAddsAuthorizationHeaderWhenAuthorizedClientIsPresent() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, TEST_URI).build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "test-token", Instant.now(), Instant.now().plusSeconds(3600));

        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);

        ClientRequest returnedRequest = WebClientConfiguration.withBearerToken(request, authorizedClientManager);

        assertEquals("Bearer test-token", returnedRequest.headers().getFirst(HttpHeaders.AUTHORIZATION),
                     "Authorization header should contain the bearer token from the authorized client");
    }

    @Test
    void withBearerTokenReturnsOriginalRequestWhenAuthorizedClientIsAbsent() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, TEST_URI).build();

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        ClientRequest returnedRequest = WebClientConfiguration.withBearerToken(request, authorizedClientManager);

        assertEquals(request, returnedRequest,
                     "Original request should be returned unchanged when there is no authorized client");
    }

    @Test
    void withBearerTokenUsesDefaultClientRegistrationIdWhenAttributeNotSet() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, TEST_URI).build();

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        WebClientConfiguration.withBearerToken(request, authorizedClientManager);

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(captor.capture());

        assertEquals("dataManagementApi", captor.getValue().getClientRegistrationId(),
                     "Default client registration id should be used when no attribute is set on the request");
    }

    @Test
    void withBearerTokenUsesClientRegistrationIdFromRequestAttributeWhenPresent() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, TEST_URI)
            .attributes(ClientAttributes.clientRegistrationId("otherApi"))
            .build();

        when(authorizedClientManager.authorize(any())).thenReturn(null);

        WebClientConfiguration.withBearerToken(request, authorizedClientManager);

        ArgumentCaptor<OAuth2AuthorizeRequest> captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(captor.capture());

        assertEquals("otherApi", captor.getValue().getClientRegistrationId(),
                     "Client registration id from the request attribute should override the default");
    }
}
