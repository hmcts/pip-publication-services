package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.jcache.JCache;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyTokenInfo;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ThirdPartyTokenCachingServiceTest {
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String ACCESS_TOKEN = UUID.randomUUID().toString();
    private static final long FUTURE_EXPIRY = System.currentTimeMillis() + 1000L;
    private static final long PAST_EXPIRY = System.currentTimeMillis();

    private static final String TOKEN_MATCHED_MESSAGE = "Returned token does not match";
    private static final String TOKEN_NULL_MESSAGE = "Returned token should be null";

    private static final ThirdPartyTokenInfo THIRD_PARTY_TOKEN_INFO = new ThirdPartyTokenInfo(ACCESS_TOKEN,
                                                                                              FUTURE_EXPIRY);
    private static final ThirdPartyTokenInfo EXPIRED_THIRD_PARTY_TOKEN_INFO = new ThirdPartyTokenInfo(ACCESS_TOKEN,
                                                                                                      PAST_EXPIRY);

    @Mock
    private JCache<String, ThirdPartyTokenInfo> tokenCache;

    @InjectMocks
    private ThirdPartyTokenCachingService thirdPartyTokenCachingService;

    @Test
    void testGetCachedTokenReturnsToken() {
        when(tokenCache.get(USER_ID)).thenReturn(THIRD_PARTY_TOKEN_INFO);

        assertThat(thirdPartyTokenCachingService.getCachedToken(USER_ID))
            .as(TOKEN_MATCHED_MESSAGE)
            .isEqualTo(ACCESS_TOKEN);
    }

    @Test
    void testGetExpiredTokenReturnsNull() {
        when(tokenCache.get(USER_ID)).thenReturn(EXPIRED_THIRD_PARTY_TOKEN_INFO);

        assertThat(thirdPartyTokenCachingService.getCachedToken(USER_ID))
            .as(TOKEN_NULL_MESSAGE)
            .isNull();
    }

    @Test
    void testGetCachedTokenNotFound() {
        when(tokenCache.get(USER_ID)).thenReturn(null);

        assertThat(thirdPartyTokenCachingService.getCachedToken(USER_ID))
            .as(TOKEN_NULL_MESSAGE)
            .isNull();
    }
}
