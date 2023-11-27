package uk.gov.hmcts.reform.pip.publication.services.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
@Profile("test")
public class RedisTestConfiguration {
    private static final String REDIS_IMAGE_NAME = "redis:latest";
    private static final int DEFAULT_REDIS_PORT = 6379;
    private static final String LOCALHOST = "localhost";

    @Bean
    public void createRedisTestContainers() {
        try (GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE_NAME))
            .withExposedPorts(DEFAULT_REDIS_PORT);) {
            redis.start();
            System.setProperty("spring.redis.host", LOCALHOST);
            System.setProperty("spring.redis.port", String.valueOf(DEFAULT_REDIS_PORT));
        }
    }
}
