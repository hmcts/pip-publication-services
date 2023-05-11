package uk.gov.hmcts.reform.pip.publication.services.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enum that contains all the templates that we use on GovNotify.
 */
@Getter
@AllArgsConstructor
public enum Templates {
    EXISTING_USER_WELCOME_EMAIL("321cbaa6-2a19-4980-87c6-fe90516db59b",
                                "Existing media account welcome email"),
    ADMIN_ACCOUNT_CREATION_EMAIL("5609165c-ae4b-4d67-acdb-8dcbd0f5fb64",
                                 "Admin account welcome email"),
    MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL("fb7c6bdd-c833-4f26-bb65-0ee4678ffd76",
                                       "Media subscription email for flat file"),
    MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL("2e0f0aca-e10d-4549-beb8-d64e68f1586a",
                                      "Media subscription email for JSON"),
    MEDIA_NEW_ACCOUNT_SETUP("e426073b-958c-42a2-a94f-bbdd1a400cb7",
                            "New media account welcome email"),
    MEDIA_DUPLICATE_ACCOUNT_EMAIL("13b058a5-82da-4331-98ff-97d3ebf66f51",
                                  "Duplicate media account email"),
    MEDIA_APPLICATION_REPORTING_EMAIL("c59c90a3-1806-4649-b4b5-b6bce8f8f72c",
                                      "Media application reporting email"),
    BAD_BLOB_EMAIL("0fbd150f-ff5b-49f0-aa34-6a6273901ceb",
                   "Unidentified blob email"),
    MEDIA_USER_VERIFICATION_EMAIL("1dea6b4b-48b6-4eb1-8b86-7031de5502d9",
                                  "Media user verification email"),
    MEDIA_USER_REJECTION_EMAIL("1988bbdd-d223-49bf-912f-ed34cb43e35e",
                               "Media account rejection email"),
    MI_DATA_REPORTING_EMAIL("f13eef24-0ae0-4970-9f56-f107308b78c5",
                            "MI data reporting email"),
    SYSTEM_ADMIN_UPDATE_EMAIL("0d47a89f-43b2-466c-b155-6ea5b82b9bbd",
                              "System admin notification email"),
    INACTIVE_USER_NOTIFICATION_EMAIL_AAD("8f1e82a9-7016-4b28-8473-20c70f9f11ba",
                                         "Inactive AAD account notification email"),
    INACTIVE_USER_NOTIFICATION_EMAIL_CFT("cca7ea18-4e6f-406f-b4d3-9e017cb53ee9",
                                         "Inactive IDAM account notification email"),
    DELETE_LOCATION_SUBSCRIPTION("929276e1-da85-4f21-9ed4-53492bedff68",
                                 "Location deletion notification email");

    private static final Map<String, Templates> LOOKUP = new ConcurrentHashMap<>();

    static {
        for (Templates value : Templates.values()) {
            LOOKUP.put(value.template, value);
        }
    }

    public final String template;
    private final String description;

    public static Templates get(String template) {
        if (LOOKUP.containsKey(template)) {
            return LOOKUP.get(template);
        }
        throw new IllegalArgumentException("Template does not exist");
    }
}
