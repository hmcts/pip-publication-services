package uk.gov.hmcts.reform.pip.publication.services.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailToSendTest {

    private EmailToSend emailToSend;

    private static final String EMAIL = "test@email.com";
    private static final String TEMPLATE = "template1";
    private static final Map<String, String> PERSONALISATION = new ConcurrentHashMap<>();
    private static final String REFERENCE_ID = "refID";

    @BeforeEach
    public void setUp() {
        PERSONALISATION.put("test", "testValue");
        emailToSend = new EmailToSend(EMAIL, TEMPLATE, PERSONALISATION, REFERENCE_ID);
    }

    @Test
    public void testEmailToSendBuildsCorrectly() {
        assertEquals(EMAIL, emailToSend.getEmailAddress(),"Email addresses should match");
        assertEquals(TEMPLATE, emailToSend.getTemplate(), "Template should match");
        assertEquals(PERSONALISATION, emailToSend.getPersonalisation(), "personalisation should match");
        assertEquals(REFERENCE_ID, emailToSend.getReferenceId(), "ReferenceId should match");
    }
}
