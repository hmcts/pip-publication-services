package uk.gov.hmcts.reform.pip.publication.services.notify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplatesTest {

    private static final String SHOULD_MATCH_MESSAGE = "Templates should match";

    private static final String EXISTING_USER_WELCOME_TEMPLATE = "321cbaa6-2a19-4980-87c6-fe90516db59b";
    private static final String MEDIA_APPLICATION_REPORTING_EMAIL = "c59c90a3-1806-4649-b4b5-b6bce8f8f72c";
    private static final String BAD_BLOB_EMAIL = "0fbd150f-ff5b-49f0-aa34-6a6273901ceb";
    private static final String MEDIA_VERIFICATION_EMAIL = "1dea6b4b-48b6-4eb1-8b86-7031de5502d9";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_AAD = "8f1e82a9-7016-4b28-8473-20c70f9f11ba";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_CFT = "cca7ea18-4e6f-406f-b4d3-9e017cb53ee9";

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

    @Test
    void testGetInactiveUserNotificationTemplateAad() {
        assertEquals(INACTIVE_USER_NOTIFICATION_EMAIL_AAD, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_AAD.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetInactiveUserNotificationTemplateCft() {
        assertEquals(INACTIVE_USER_NOTIFICATION_EMAIL_CFT, Templates.INACTIVE_USER_NOTIFICATION_EMAIL_CFT.template,
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetEnumFromTemplate() {
        assertEquals(Templates.EXISTING_USER_WELCOME_EMAIL,
                     Templates.get(Templates.EXISTING_USER_WELCOME_EMAIL.template),
                     SHOULD_MATCH_MESSAGE);
    }

    @Test
    void testGetEnumFromTemplateNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            Templates.get("NotFoundTemplate"), "Expected exception has not been thrown");
        assertEquals("Template does not exist", ex.getMessage(), "Exception message does not match");
    }
}
