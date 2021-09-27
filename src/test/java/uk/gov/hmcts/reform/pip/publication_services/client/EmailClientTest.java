package uk.gov.hmcts.reform.pip.publication_services.client;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
public class EmailClientTest {

    public static final String API_KEY= "7033ceaf-4fdf-4c72-84a8-d916762ddbeb";

    @Autowired
    private EmailClient emailClient;

    @Test
    public void testClientHasCorrectApiKey() {
        assertEquals(API_KEY, emailClient.getApiKey());
    }
}
