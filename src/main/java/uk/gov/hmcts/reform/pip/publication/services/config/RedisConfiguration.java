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

@Configuration
public class RedisConfiguration {
    private static final String CACHE = "publication-services-emails-cache";
    private static final String LOCAL = "local";
    private static final String REDIS_PROTOCOL_PREFIX = "rediss://";
    private static final String LOCAL_REDIS_PROTOCOL_PREFIX = "redis://";

    @Value("${env-name}")
    private String envName;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public Config config() {
        String connectionString = (LOCAL.equals(envName) ? LOCAL_REDIS_PROTOCOL_PREFIX : REDIS_PROTOCOL_PREFIX)
            + redisPassword + '@' + redisHost + ":" + redisPort;
        Config config = new Config();
        config.useSingleServer().setAddress(connectionString);
        return config;
    }

    @Bean
    public CacheManager cacheManager(Config config) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        manager.createCache(CACHE, RedissonConfiguration.fromConfig(config));
        return manager;
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
