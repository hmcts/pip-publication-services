package uk.gov.hmcts.reform.pip.publication.services.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RateLimitConfiguration {
    @Autowired
    public ProxyManager<String> buckets;

    public Bucket resolveBucket(String key) {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser();
        return buckets.builder()
            .build(key, configSupplier);
    }

    private Supplier<BucketConfiguration> getConfigSupplierForUser() {
        Bandwidth capacity = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return () -> BucketConfiguration.builder()
            .addLimit(capacity)
            .build();
    }
}
