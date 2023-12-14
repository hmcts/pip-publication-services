package uk.gov.hmcts.reform.pip.publication.services.config;

import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;
import com.giffing.bucket4j.spring.boot.starter.config.cache.jcache.JCacheCacheResolver;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
public class RedisConfiguration {
    private static final String CACHE = "publication-services-emails-cache";
    private static final String LOCAL = "local";
    private static final String TLS_REDIS_PROTOCOL_PREFIX = "rediss://";
    private static final String LOCAL_REDIS_PROTOCOL_PREFIX = "redis://";

    @Value("${env-name}")
    private String envName;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${rate-limit.cache.expiry-in-minute}")
    private int cacheExpiry;

    @Bean
    public Config redissonConfiguration() {
        String connectionString = (LOCAL.equals(envName) ? LOCAL_REDIS_PROTOCOL_PREFIX : TLS_REDIS_PROTOCOL_PREFIX)
            + (redisPassword.isEmpty() ? "" : ":" + redisPassword + "@")
            + redisHost + ":" + redisPort;
        Config config = new Config();
        config.useSingleServer().setAddress(connectionString);
        return config;
    }

    @Bean
    public MutableConfiguration<String, String> jcacheConfiguration() {
        MutableConfiguration<String, String> config = new MutableConfiguration<>();
        config.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(MINUTES, cacheExpiry)));
        return config;
    }

    @Bean
    public CacheManager cacheManager(Config redissonConfig, MutableConfiguration<String, String> jcacheConfiguration) {
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
        cacheManager.createCache(CACHE, RedissonConfiguration.fromConfig(redissonConfig, jcacheConfiguration));
        return cacheManager;
    }

    @Bean
    ProxyManager<String> proxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache(CACHE));
    }

    @Bean
    @Primary
    public SyncCacheResolver bucket4jCacheResolver(CacheManager cacheManager) {
        return new JCacheCacheResolver(cacheManager);
    }
}
