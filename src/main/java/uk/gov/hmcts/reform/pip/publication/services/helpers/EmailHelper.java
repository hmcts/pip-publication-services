package uk.gov.hmcts.reform.pip.publication.services.helpers;

/**
 * Helper class which provides any helper methods.
 */
public final class EmailHelper {

    private EmailHelper() {
        //Private constructor for utility classes.
    }

    /**
     * Method which helps mask emails.
     * @param email The email address to mask.
     * @return The masked email address.
     */
    public static String maskEmail(String email) {
        return email.replaceAll("(^([^@])|(?!^)\\G)[^@]", "$1*");
    }

}
