package uk.gov.hmcts.reform.pip.publication.services.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.BatchEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.MediaApplicationReportingEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.MiDataReportingEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.SystemAdminUpdateEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.UnidentifiedBlobEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.FlatFileSubscriptionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.LocationSubscriptionDeletionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.RawDataSubscriptionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.AdminWelcomeEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.InactiveUserNotificationEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.MediaAccountRejectionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.MediaDuplicatedAccountEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.MediaUserVerificationEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.MediaWelcomeEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.OtpEmailGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enum that contains all the templates that we use on GovNotify.
 */
@Getter
@AllArgsConstructor
public enum Templates {
    EXISTING_USER_WELCOME_EMAIL("cc1b744d-6aa1-4410-9f53-216f8bd3298f",
                                "Existing media account welcome email",
                                EmailLimit.STANDARD,
                                new MediaWelcomeEmailGenerator()),
    ADMIN_ACCOUNT_CREATION_EMAIL("0af670d6-024a-4fe5-ae2d-b908f69b0fc0",
                                 "Admin account welcome email",
                                 EmailLimit.STANDARD,
                                 new AdminWelcomeEmailGenerator()),
    MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL("1fa5db55-5c75-4c50-a37a-5f3883e9f672",
                                       "Media subscription email for flat file",
                                       EmailLimit.HIGH,
                                       new FlatFileSubscriptionEmailGenerator()),
    MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL("55af1beb-2373-4359-b809-b96d8044fa2d",
                                      "Media subscription email for JSON",
                                      EmailLimit.HIGH,
                                      new RawDataSubscriptionEmailGenerator()),
    MEDIA_NEW_ACCOUNT_SETUP("689c0183-0461-423e-a542-de513a93a5b7",
                            "New media account welcome email",
                            EmailLimit.STANDARD,
                            new MediaWelcomeEmailGenerator()),
    MEDIA_DUPLICATE_ACCOUNT_EMAIL("13b058a5-82da-4331-98ff-97d3ebf66f51",
                                  "Duplicate media account email",
                                  EmailLimit.STANDARD,
                                  new MediaDuplicatedAccountEmailGenerator()),
    MEDIA_APPLICATION_REPORTING_EMAIL("c59c90a3-1806-4649-b4b5-b6bce8f8f72c",
                                      "Media application reporting email",
                                      EmailLimit.STANDARD,
                                      new MediaApplicationReportingEmailGenerator()),
    BAD_BLOB_EMAIL("0fbd150f-ff5b-49f0-aa34-6a6273901ceb",
                   "Unidentified blob email",
                   EmailLimit.STANDARD,
                   new UnidentifiedBlobEmailGenerator()),
    MEDIA_USER_VERIFICATION_EMAIL("1dea6b4b-48b6-4eb1-8b86-7031de5502d9",
                                  "Media user verification email",
                                  EmailLimit.STANDARD,
                                  new MediaUserVerificationEmailGenerator()),
    MEDIA_USER_REJECTION_EMAIL("1988bbdd-d223-49bf-912f-ed34cb43e35e",
                               "Media account rejection email",
                               EmailLimit.STANDARD,
                               new MediaAccountRejectionEmailGenerator()),
    MI_DATA_REPORTING_EMAIL("f13eef24-0ae0-4970-9f56-f107308b78c5",
                            "MI data reporting email",
                            EmailLimit.STANDARD,
                            new MiDataReportingEmailGenerator()),
    SYSTEM_ADMIN_UPDATE_EMAIL("b3c0a60f-34ee-4bfa-857d-7ccbd678cf0c",
                              "System admin notification email",
                              EmailLimit.HIGH,
                              null,
                              new SystemAdminUpdateEmailGenerator()),
    INACTIVE_USER_NOTIFICATION_EMAIL_AAD("8f1e82a9-7016-4b28-8473-20c70f9f11ba",
                                         "Inactive AAD account notification email",
                                         EmailLimit.STANDARD,
                                         new InactiveUserNotificationEmailGenerator()),
    INACTIVE_USER_NOTIFICATION_EMAIL_CFT("cca7ea18-4e6f-406f-b4d3-9e017cb53ee9",
                                         "Inactive CFT IDAM account notification email",
                                         EmailLimit.STANDARD,
                                         new InactiveUserNotificationEmailGenerator()),

    INACTIVE_USER_NOTIFICATION_EMAIL_CRIME("710a1ea4-226d-4e94-a8f7-5a102bb31612",
                                           "Inactive Crime IDAM account notification email",
                                           EmailLimit.STANDARD,
                                           new InactiveUserNotificationEmailGenerator()),
    DELETE_LOCATION_SUBSCRIPTION("929276e1-da85-4f21-9ed4-53492bedff68",
                                 "Location deletion notification email",
                                 EmailLimit.HIGH,
                                 null,
                                 new LocationSubscriptionDeletionEmailGenerator()),
    OTP_EMAIL("c51dc591-e956-43b2-8cc2-ac32bbcece3b",
              "B2C OTP email",
              EmailLimit.HIGH,
              new OtpEmailGenerator());

    private static final Map<String, Templates> LOOKUP = new ConcurrentHashMap<>();

    static {
        for (Templates value : values()) {
            LOOKUP.put(value.template, value);
        }
    }

    private final String template;
    private final String description;
    private final EmailLimit emailLimit;
    private final EmailGenerator emailGenerator;
    private BatchEmailGenerator batchEmailGenerator;

    Templates(String template, String description, EmailLimit emailLimit, EmailGenerator emailGenerator) {
        this.template = template;
        this.description = description;
        this.emailLimit = emailLimit;
        this.emailGenerator = emailGenerator;
    }

    public static Templates get(String template) {
        if (LOOKUP.containsKey(template)) {
            return LOOKUP.get(template);
        }
        throw new IllegalArgumentException("Template does not exist");
    }
}
