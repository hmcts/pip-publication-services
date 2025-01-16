package uk.gov.hmcts.pip.publication.services;

import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient;
import uk.gov.hmcts.pip.publication.services.utils.FunctionalTestBase;
import uk.gov.hmcts.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
class ReportingNotificationEmailTests extends FunctionalTestBase {
    private static final String NOTIFY_URL = "/notify";
    private static final String MEDIA_APPLICATION_REPORTING_EMAIL_URL = NOTIFY_URL + "/media/report";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = NOTIFY_URL + "/unidentified-blob";
    private static final String MI_DATA_REPORTING_EMAIL_URL = NOTIFY_URL + "/mi/report";

    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-ps-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String EMAIL = TEST_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String FULL_NAME = "Test name";
    private static final String EMPLOYER = "Test employer";
    private static final String IMAGE = "Test image";
    private static final String IMAGE_NAME = "image.pdf";
    private static final String STATUS = "PENDING";
    private static final UUID ID = UUID.randomUUID();
    private static final String PROVENANCE = "MANUAL_UPLOAD";
    private static final String LOCATION_ID = "123";
    private static final String PI_TEAM_EMAIL = "publicationsinformation@justice.gov.uk";
    private static final String NOTIFY_LINK = "https://documents.service.gov.uk";

    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";
    private static final String EMAIL_SUBJECT_ERROR = "Email subject does not match";
    private static final String EMAIL_BODY_ERROR = "Email body does not match";
    private static final String EMAIL_LINK_ERROR = "Email link does not match";

    @Autowired
    private EmailNotificationClient notificationClient;

    @Test
    void shouldSendMediaApplicationReportingEmail() throws NotificationClientException {
        MediaApplication mediaApplication = new MediaApplication(UUID.randomUUID(), FULL_NAME, EMAIL, EMPLOYER,
                                                                 IMAGE, IMAGE_NAME, LocalDateTime.now(), STATUS,
                                                                 LocalDateTime.now());

        final Response response = doPostRequest(MEDIA_APPLICATION_REPORTING_EMAIL_URL,
                                                Map.of(AUTHORIZATION, bearerToken),
                                                List.of(mediaApplication));

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

        Notification notification = readNotification(referenceId);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(PI_TEAM_EMAIL);

        assertThat(notification.getSubject().get())
            .as(EMAIL_SUBJECT_ERROR)
            .contains("Media Account Assessment Reporting");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Here is this month's P&I report for new media account assessment statistics.");

        assertThat(notification.getBody())
            .as(EMAIL_LINK_ERROR)
            .contains(NOTIFY_LINK);
    }

    @Test
    void shouldSendUnidentifiedBlobEmail() throws NotificationClientException {
        NoMatchArtefact noMatchArtefact = new NoMatchArtefact(ID, PROVENANCE, LOCATION_ID);

        final Response response = doPostRequest(
            UNIDENTIFIED_BLOB_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken),
            List.of(noMatchArtefact)
        );

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

        Notification notification = readNotification(referenceId);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(PI_TEAM_EMAIL);

        assertThat(notification.getSubject().get())
            .as(EMAIL_SUBJECT_ERROR)
            .contains("Unrecognised Blobs are being received into CaTh that need attention");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("The below Provenance and Location IDs have been sent into CaTH as part of a Blob payload.");

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(LOCATION_ID);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(PROVENANCE);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(ID.toString());
    }

    @Test
    void shouldSendMiDataReportingEmail() throws NotificationClientException {
        final Response response = doPostRequestWithoutBody(
            MI_DATA_REPORTING_EMAIL_URL,
            Map.of(AUTHORIZATION, bearerToken)
        );

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

        Notification notification = readNotification(referenceId);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(PI_TEAM_EMAIL);
    }

    private Notification readNotification(String referenceId) throws NotificationClientException {
        Awaitility.with()
            .pollInterval(1, SECONDS)
            .await()
            .until(() -> {
                NotificationList notificationList = notificationClient.getNotifications(
                    null, NOTIFICATION_TYPE, referenceId, null
                );
                return notificationList != null
                    && notificationList.getNotifications().size() == 1;
            });

        NotificationList notificationList = notificationClient.getNotifications(
            null, NOTIFICATION_TYPE, referenceId, null
        );

        return notificationList.getNotifications().get(0);
    }
}
