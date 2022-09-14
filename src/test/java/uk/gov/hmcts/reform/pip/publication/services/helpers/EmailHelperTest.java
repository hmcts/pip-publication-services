package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailHelperTest {

    @Test
    void testMaskEmail() {
        Assertions.assertEquals(
            EmailHelper.maskEmail("testUser@email.com"), "t*******@email.com", "Email was not masked correctly");
    }

    @Test
    void testMaskEmailNotValidEmail() {
        Assertions.assertEquals(EmailHelper.maskEmail("abcde"), "a****",
                                "Email was not masked correctly");
    }

    @Test
    void testMaskEmailEmptyString() {
        Assertions.assertEquals(
            EmailHelper.maskEmail(""), "", "Email was not masked correctly");
    }

}
