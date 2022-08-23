package uk.gov.hmcts.reform.pip.publication.services.notify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplatesTest {

    private static final String SHOULD_MATCH_MESSAGE = "Templates should match";

    private static final String NEW_USER_WELCOME_TEMPLATE = "b708c2dc-5794-4468-a8bf-f798fe1f91bc";
    private static final String EXISTING_USER_WELCOME_TEMPLATE = "321cbaa6-2a19-4980-87c6-fe90516db59b";
    private static final String MEDIA_APPLICATION_REPORTING_EMAIL = "c59c90a3-1806-4649-b4b5-b6bce8f8f72c";
    private static final String BAD_BLOB_EMAIL = "0fbd150f-ff5b-49f0-aa34-6a6273901ceb";
    private static final String MEDIA_VERIFICATION_EMAIL = "1dea6b4b-48b6-4eb1-8b86-7031de5502d9";


    @Test
    void testGetNewUserWelcomeEmailTemplate() {
        assertEquals(NEW_USER_WELCOME_TEMPLATE, Templates.NEW_USER_WELCOME_EMAIL.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetExistingUserWelcomeEmailTemplate() {
        assertEquals(EXISTING_USER_WELCOME_TEMPLATE, Templates.EXISTING_USER_WELCOME_EMAIL.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetMediaApplicationReportingEmailTemplate() {
        assertEquals(MEDIA_APPLICATION_REPORTING_EMAIL, Templates.MEDIA_APPLICATION_REPORTING_EMAIL.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetBadBlobEmailTemplate() {
        assertEquals(BAD_BLOB_EMAIL, Templates.BAD_BLOB_EMAIL.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetMediaVerificationTemplate() {
        assertEquals(MEDIA_VERIFICATION_EMAIL, Templates.MEDIA_USER_VERIFICATION_EMAIL.template,
                     SHOULD_MATCH_MESSAGE);
    }
}
