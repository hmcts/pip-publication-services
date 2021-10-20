package uk.gov.hmcts.reform.pip.publication.services.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
class EmailClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailClientTest.class);

    @Value("${notify.api.key}")
    private String mockApiKey;

    @Autowired
    private EmailClient emailClient;

    @Test
    void testClientHasCorrectApiKey() {
        LOGGER.warn("API KEY: " + mockApiKey);
        assertTrue(mockApiKey.contains(emailClient.getApiKey()), "Keys should match");
    }
}
