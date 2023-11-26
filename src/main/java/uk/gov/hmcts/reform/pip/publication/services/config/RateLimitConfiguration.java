package uk.gov.hmcts.reform.pip.publication.services.config;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;

import java.time.Duration;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit.STANDARD;

@Configuration
public class RateLimitConfiguration {
    @Autowired
    public ProxyManager<String> buckets;

    @Value("${rate-limit.email.capacity.standard}")
    private Integer standardEmailCapacity;

    @Value("${rate-limit.email.capacity.high}")
    private Integer highEmailCapacity;

    @Value("${rate-limit.email.interval-in-minutes}")
    private Integer rateLimitInterval;

    public Bucket resolveBucket(String key, EmailLimit emailLimit) {
        Integer emailCapacity = STANDARD.equals(emailLimit) ? standardEmailCapacity : highEmailCapacity;
        Supplier<BucketConfiguration> configSupplier = getBucketConfiguration(emailCapacity);
        return buckets.builder()
            .build(key, configSupplier);
    }

    private Supplier<BucketConfiguration> getBucketConfiguration(Integer emailCapacity) {
        Refill refill = Refill.greedy(emailCapacity, Duration.ofMinutes(rateLimitInterval));
        Bandwidth bandwidth = Bandwidth.classic(emailCapacity, refill);
        return () -> BucketConfiguration.builder()
            .addLimit(bandwidth)
            .build();
    }
}
