package uk.gov.hmcts.reform.pip.publication.services.service;

import com.azure.core.exception.HttpResponseException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.AzureSecretReadException;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyTokenInfo;

import javax.cache.Cache;
import javax.cache.CacheManager;

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
    private static final String TOKEN_CACHE = "third-party_token-cache";
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final WebClient webClient;
    private final SecretClient secretClient;
    private final CacheManager cacheManager;

    @Autowired
    public ThirdPartyOauthService(WebClient webClientInsecure, SecretClient secretClient, CacheManager tokenCacheManager) {
        this.webClient = webClientInsecure;
        this.secretClient = secretClient;
        this.cacheManager = tokenCacheManager;
    }

    public String getApiAccessToken(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration) {
        Cache<String, ThirdPartyTokenInfo> tokenCache = cacheManager.getCache(TOKEN_CACHE);
        String cacheKey = thirdPartyOauthConfiguration.getUserId().toString();
        ThirdPartyTokenInfo cachedToken = tokenCache.get(cacheKey);

        if (cachedToken != null && !cachedToken.isExpired()) {
            return cachedToken.getAccessToken();
        }

        return requestApiAccessToken(thirdPartyOauthConfiguration, tokenCache);
    }

    public String requestApiAccessToken(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration,
                                         Cache<String, ThirdPartyTokenInfo> tokenCache) {
        String testKey = getSecretValue("b2c-client-id");

//        String clientId = getSecretValue(thirdPartyOauthConfiguration.getClientIdKey());
//        String clientSecret = getSecretValue(thirdPartyOauthConfiguration.getClientSecretKey());
//        String scope = getSecretValue(thirdPartyOauthConfiguration.getScopeKey());

        try {
            WebClient.RequestHeadersSpec<?> req = webClient.post()
                .uri(thirdPartyOauthConfiguration.getTokenUrl())
                .body(BodyInserters.fromFormData(GRANT_TYPE, CLIENT_CREDENTIALS)
                          .with(CLIENT_ID, thirdPartyOauthConfiguration.getClientIdKey())
                          .with(CLIENT_SECRET, thirdPartyOauthConfiguration.getClientSecretKey())
                          .with(SCOPE, thirdPartyOauthConfiguration.getScopeKey()));

            return req.retrieve()
                .bodyToMono(JsonNode.class)
                .map(res -> {
                    String accessToken = res.get(ACCESS_TOKEN).textValue();
                    if (res.get(EXPIRES_IN) != null) {
                        long expiresIn = res.get(EXPIRES_IN).asLong();
                        long expiryMillis = System.currentTimeMillis() + (expiresIn - TOKEN_EXPIRY_BUFFER_SECONDS) * 1000L;
                        tokenCache.put(thirdPartyOauthConfiguration.getUserId().toString(),
                                       new ThirdPartyTokenInfo(accessToken, expiryMillis));
                    }
                    return accessToken;
                }).block();
        } catch (Exception ex) {
            log.error(writeLog("Failed to generate access token for third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId() + ex.getMessage()));
            return null;
        }
    }

    private String getSecretValue(String secretKey) {
        try {
            return secretClient.getSecret(secretKey).getValue();
        } catch (HttpResponseException e) {
            throw new AzureSecretReadException("Failed to retrieve secret with key: " + secretKey);
        }
    }
}
