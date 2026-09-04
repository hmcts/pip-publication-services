package uk.gov.hmcts.reform.pip.publication.services.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailLimit;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.BatchEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.MediaApplicationReportingEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.SystemAdminUpdateEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting.UnidentifiedBlobEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.FlatFileSubscriptionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.LocationSubscriptionDeletionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.RawDataSubscriptionEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.RawDataSubscriptionEmailGeneratorV2;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.InactiveUserNotificationEmailGenerator;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount.MediaAccountDeletionEmailGenerator;
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
    MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL("3ec5ef89-4d0b-4142-aa54-3d3e68e201d4",
                                       "Media subscription email for flat file",
                                       EmailLimit.HIGH,
                                       new FlatFileSubscriptionEmailGenerator()),
    @Deprecated
    MEDIA_SUBSCRIPTION_PDF_EXCEL_EMAIL("4017c40f-0644-4b02-acd2-e00a1ece3b85",
                                      "Media subscription email for JSON with PDF and Excel",
                                      EmailLimit.HIGH,
                                      new RawDataSubscriptionEmailGenerator()),
    @Deprecated
    MEDIA_SUBSCRIPTION_PDF_EMAIL("e551a0c1-91e7-4871-a540-1e7101b70f14",
                                       "Media subscription email for JSON with PDF",
                                       EmailLimit.HIGH,
                                       new RawDataSubscriptionEmailGenerator()),
    @Deprecated
    MEDIA_SUBSCRIPTION_EXCEL_EMAIL("e03108e1-db29-40d3-90f2-bf8f6c233c35",
                                 "Media subscription email for JSON with Excel",
                                 EmailLimit.HIGH,
                                 new RawDataSubscriptionEmailGenerator()),
    @Deprecated
    MEDIA_SUBSCRIPTION_NO_DOWNLOAD_LINK_EMAIL("072fa7fd-ac23-4a99-be9a-70153374c66e",
                                 "Media subscription email for JSON with no download link",
                                 EmailLimit.HIGH,
                                 new RawDataSubscriptionEmailGenerator()),
    MEDIA_SUBSCRIPTION_PDF_EXCEL_EMAIL_V2("cbbbe6fb-c7a1-4dd9-b606-e5bac1aaa990",
                                          "Media subscription email for JSON with PDF and Excel",
                                          EmailLimit.HIGH,
                                          new RawDataSubscriptionEmailGeneratorV2()),
    MEDIA_SUBSCRIPTION_PDF_EMAIL_V2("2355bdc0-9e5d-4cac-a6b0-761306e9f6c5",
                                    "Media subscription email for JSON with PDF",
                                    EmailLimit.HIGH,
                                    new RawDataSubscriptionEmailGeneratorV2()),
    MEDIA_SUBSCRIPTION_EXCEL_EMAIL_V2("d0e48435-93ee-418e-8c92-9aaf69070297",
                                      "Media subscription email for JSON with Excel",
                                      EmailLimit.HIGH,
                                      new RawDataSubscriptionEmailGeneratorV2()),
    MEDIA_SUBSCRIPTION_NO_DOWNLOAD_LINK_EMAIL_V2("7e285fe0-f285-4bed-8cb1-c97ac6782d4a",
                                                 "Media subscription email for JSON with no download link",
                                                 EmailLimit.HIGH,
                                                 new RawDataSubscriptionEmailGeneratorV2()),
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
    INACTIVE_MEDIA_USER_DELETION_EMAIL("b08a6094-edc4-4d76-beca-f8937b63e879",
                                       "Media account deletion email",
                                       EmailLimit.STANDARD,
                                       new MediaAccountDeletionEmailGenerator()),
    SYSTEM_ADMIN_UPDATE_EMAIL("b3c0a60f-34ee-4bfa-857d-7ccbd678cf0c",
                              "System admin notification email",
                              EmailLimit.HIGH,
                              null,
                              new SystemAdminUpdateEmailGenerator()),
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
