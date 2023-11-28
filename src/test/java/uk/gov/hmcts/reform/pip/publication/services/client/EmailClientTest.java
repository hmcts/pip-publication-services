package uk.gov.hmcts.reform.pip.publication.services.client;

import com.redis.testcontainers.RedisContainer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientTestConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, WebClientTestConfiguration.class})
@DirtiesContext
@SuppressWarnings("PMD.ImmutableField")
@Testcontainers
@ActiveProfiles("test")
class EmailClientTest {

    @Container
    private static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }

    @Value("${notify.api.key}")
    private String mockApiKey;

    @Autowired
    private EmailClient emailClient;

    @Test
    void testClientHasCorrectApiKey() {
        assertTrue(mockApiKey.contains(emailClient.getApiKey()), "Keys should match");
    }
}
