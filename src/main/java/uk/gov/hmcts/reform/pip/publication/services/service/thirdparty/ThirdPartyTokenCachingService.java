package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyTokenInfo;

import javax.cache.Cache;

@Service
public class ThirdPartyTokenCachingService {
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final Cache<String, ThirdPartyTokenInfo> tokenCache;

    @Autowired
    public ThirdPartyTokenCachingService(Cache<String, ThirdPartyTokenInfo> tokenCache) {
        this.tokenCache = tokenCache;
    }

    public String getCachedToken(String userId) {
        if (userId != null) {
            ThirdPartyTokenInfo cachedToken = tokenCache.get(userId);
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.getAccessToken();
            }
        }
        return null;
    }

    public void cacheToken(String userId, String accessToken, Long expiresIn) {
        if (userId != null && accessToken != null) {
            long expiryMillis = expiresIn > TOKEN_EXPIRY_BUFFER_SECONDS
                ? System.currentTimeMillis() + (expiresIn - TOKEN_EXPIRY_BUFFER_SECONDS) * 1000L
                : 0;
            ThirdPartyTokenInfo tokenInfo = new ThirdPartyTokenInfo(accessToken, expiryMillis);
            tokenCache.put(userId, tokenInfo);
        }
    }
}
