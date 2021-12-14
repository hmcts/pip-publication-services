package uk.gov.hmcts.reform.pip.publication.services.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {"NOTIFY_API_TESTING_KEY=7033ceaf-4fdf-4c72-84a8-d916762ddbeb"})
class EmailClientTest {

    private static final String API_KEY = "7033ceaf-4fdf-4c72-84a8-d916762ddbeb";

    @Autowired
    private EmailClient emailClient;

    @Test
    void testClientHasCorrectApiKey() {
        assertEquals(API_KEY, emailClient.getApiKey(), "Keys should match");
    }
}
