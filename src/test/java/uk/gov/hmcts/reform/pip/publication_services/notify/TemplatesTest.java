package uk.gov.hmcts.reform.pip.publication_services.notify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplatesTest {

    private static final String NEW_USER_WELCOME_TEMPLATE = "b708c2dc-5794-4468-a8bf-f798fe1f91bc";
    private static final String EXISTING_USER_WELCOME_TEMPLATE = "321cbaa6-2a19-4980-87c6-fe90516db59b";


    @Test
    public void testGetNewUserWelcomeEmailTemplate() {
        assertEquals(NEW_USER_WELCOME_TEMPLATE, Templates.NEW_USER_WELCOME_EMAIL.template, "Templates should match");
    }

    @Test
    public void testGetExistingUserWelcomeEmailTemplate() {
        assertEquals(EXISTING_USER_WELCOME_TEMPLATE, Templates.EXISTING_USER_WELCOME_EMAIL.template, "Templates should match");
    }
}
