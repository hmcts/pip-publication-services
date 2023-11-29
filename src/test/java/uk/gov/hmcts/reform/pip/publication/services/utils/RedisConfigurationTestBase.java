package uk.gov.hmcts.reform.pip.publication.services.utils;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SuppressWarnings("checkstyle:HideUtilityClassConstructorCheck")
public class RedisConfigurationTestBase {
    private static final String REDIS_IMAGE_NAME = "redis:latest";
    private static final int REDIS_PORT = 6379;

    @Container
    private static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse(REDIS_IMAGE_NAME))
        .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT).toString());
    }
}
