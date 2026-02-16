package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.service.KeyVaultService;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class ThirdPartyOauthService {
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String SCOPE = "scope";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";

    private final WebClient webClient;
    private final KeyVaultService keyVaultService;
    private final ThirdPartyTokenCachingService thirdPartyTokenCachingService;

    @Autowired
    public ThirdPartyOauthService(WebClient webClientThirdParty, KeyVaultService keyVaultService,
                                  ThirdPartyTokenCachingService thirdPartyTokenCachingService) {
        this.webClient = webClientThirdParty;
        this.keyVaultService = keyVaultService;
        this.thirdPartyTokenCachingService = thirdPartyTokenCachingService;
    }

    public String getApiAccessToken(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration) {
        String accessToken = thirdPartyTokenCachingService.getCachedToken(thirdPartyOauthConfiguration.getUserId()
                                                                              .toString());
        if (accessToken == null) {
            return requestApiAccessToken(thirdPartyOauthConfiguration);
        }
        return accessToken;
    }

    public String requestApiAccessToken(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration) {
        String clientId = keyVaultService.getSecretValue(thirdPartyOauthConfiguration.getClientIdKey());
        String clientSecret = keyVaultService.getSecretValue(thirdPartyOauthConfiguration.getClientSecretKey());
        String scope = keyVaultService.getSecretValue(thirdPartyOauthConfiguration.getScopeKey());

        try {
            WebClient.RequestHeadersSpec<?> req = webClient.post()
                .uri(thirdPartyOauthConfiguration.getTokenUrl())
                .body(BodyInserters.fromFormData(GRANT_TYPE, CLIENT_CREDENTIALS)
                          .with(CLIENT_ID, clientId)
                          .with(CLIENT_SECRET, clientSecret)
                          .with(SCOPE, scope));

            return req.retrieve()
                .bodyToMono(JsonNode.class)
                .map(res -> {
                    String accessToken = res.get(ACCESS_TOKEN).textValue();
                    Long expiresIn = res.get(EXPIRES_IN) != null ? res.get(EXPIRES_IN).asLong() : 0;
                    thirdPartyTokenCachingService.cacheToken(thirdPartyOauthConfiguration.getUserId().toString(),
                                                             accessToken, expiresIn);
                    return accessToken;
                }).block();
        } catch (Exception ex) {
            log.error(writeLog("Failed to generate access token for third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId() + ex.getMessage()));
            return null;
        }
    }
}
