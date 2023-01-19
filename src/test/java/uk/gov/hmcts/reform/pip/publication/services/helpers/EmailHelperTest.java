package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailHelperTest {

    @Test
    void testMaskEmail() {
        Assertions.assertEquals("t*******@email.com", EmailHelper.maskEmail("testUser@email.com"),
                                "Email was not masked correctly");
    }

    @Test
    void testMaskEmailNotValidEmail() {
        Assertions.assertEquals("a****", EmailHelper.maskEmail("abcde"), "Email was not masked correctly");
    }

    @Test
    void testMaskEmailEmptyString() {
        Assertions.assertEquals("", EmailHelper.maskEmail(""), "Email was not masked correctly");
    }

}
