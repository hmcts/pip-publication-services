package uk.gov.hmcts.pip.publication.services.utils;

import java.util.Random;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestUtil {

    private TestUtil() {
    }

    public static String randomLocationId() {
        Random number = new Random(System.currentTimeMillis());
        Integer randomNumber = 10_000 + number.nextInt(20_000);
        return randomNumber.toString();
    }
}
