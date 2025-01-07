package uk.gov.hmcts.reform.pip.publication.services.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("integration")
@SpringBootTest
@DirtiesContext
class EmailClientTest {

    @Value("${notify.api.key}")
    private String mockApiKey;

    @Autowired
    private EmailClient emailClient;

    @Test
    void testClientHasCorrectApiKey() {
        assertTrue(mockApiKey.contains(emailClient.getApiKey()), "Keys should match");
    }
}
