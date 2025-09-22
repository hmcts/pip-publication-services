package uk.gov.hmcts.pip.publication.services;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.assertj.core.api.AssertionsForClassTypes;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient;
import uk.gov.hmcts.pip.publication.services.utils.FunctionalTestBase;
import uk.gov.hmcts.pip.publication.services.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactType;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.NotificationList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.hmcts.pip.publication.services.utils.EmailNotificationClient.NOTIFICATION_TYPE;
import static uk.gov.hmcts.pip.publication.services.utils.TestUtil.randomLocationId;

@SuppressWarnings({"PMD.ExcessiveImports"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class, EmailNotificationClient.class})
class SubscriptionNotificationEmailTests extends FunctionalTestBase {

    @Autowired
    private EmailNotificationClient notificationClient;

    @Value("${test-system-admin-id}")
    private String systemAdminUserId;

    private static final String BULK_SUBSCRIPTION_URL = "/notify/subscription";
    private static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";
    private static final String TESTING_SUPPORT_PUBLICATION_URL = "/testing-support/publication";
    private static final String PUBLICATION_URL = "/publication";

    private static final String TEST_USER_EMAIL_PREFIX = String.format(
        "pip-ps-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_EMAIL = TEST_USER_EMAIL_PREFIX + "@justice.gov.uk";
    private static final ArtefactType ARTEFACT_TYPE = ArtefactType.LIST;
    private static final String PROVENANCE = "MANUAL_UPLOAD";
    private static final String SOURCE_ARTEFACT_ID = "sourceArtefactId";
    private static final LocalDateTime DISPLAY_FROM = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private static final Language LANGUAGE = Language.ENGLISH;
    private static final ListType LIST_TYPE = ListType.SJP_PRESS_LIST;

    private static final String TEST_CASE_NUMBER = "987654321";
    private static final String TEST_CASE_URN = "Test Case URN";
    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    private static final String EMAIL_SUBJECT_TEXT = " – your email subscriptions";
    private static final String EMAIL_BODY = "Manage your subscriptions, view lists and additional case information";
    private static final String EMAIL_ADDRESS_ERROR = "Email address does not match";
    private static final String EMAIL_SUBJECT_ERROR = "Email subject does not match";
    private static final String EMAIL_NAME_ERROR = "Name in email body does not match";
    private static final String EMAIL_BODY_ERROR = "Email body does not match";
    private static final String BEARER = "Bearer ";
    private static final String DOWNLOAD_PDF_TEXT = "Download the case list as a PDF.";
    private static final LocalDateTime CONTENT_DATE = LocalDateTime.now().toLocalDate().atStartOfDay()
        .truncatedTo(ChronoUnit.SECONDS);
    private UUID jsonArtefactId;
    private UUID flatFileArtefactId;
    private UUID jsonArtefactIdWelsh;

    private String getJsonString(String file) throws IOException {
        try (InputStream jsonFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(file)) {
            return new String(jsonFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Artefact uploadArtefact(String language, String listType, String jsonFile) throws IOException {
        Map<String, String> headerMapUploadJsonFile = new ConcurrentHashMap<>();
        headerMapUploadJsonFile.put(HttpHeaders.AUTHORIZATION, dataManagementAccessToken);
        headerMapUploadJsonFile.put("x-type", ARTEFACT_TYPE.toString());
        headerMapUploadJsonFile.put("x-provenance", PROVENANCE);
        headerMapUploadJsonFile.put("x-source-artefact-id", SOURCE_ARTEFACT_ID);
        headerMapUploadJsonFile.put("x-display-from", DISPLAY_FROM.toString());
        headerMapUploadJsonFile.put("x-display-to", DISPLAY_FROM.plusDays(1).toString());
        headerMapUploadJsonFile.put("x-court-id", LOCATION_ID);
        headerMapUploadJsonFile.put("x-list-type", listType);
        headerMapUploadJsonFile.put("x-content-date", CONTENT_DATE.toString());
        headerMapUploadJsonFile.put("x-sensitivity", "PUBLIC");
        headerMapUploadJsonFile.put("x-language", language);
        headerMapUploadJsonFile.put("x-requester-id", systemAdminUserId);
        headerMapUploadJsonFile.put("Content-Type", "application/json");

        final Response responseUploadJson = doDataManagementPostRequest(
            PUBLICATION_URL,
            headerMapUploadJsonFile, getJsonString(jsonFile)
        );

        AssertionsForClassTypes.assertThat(responseUploadJson.getStatusCode()).isEqualTo(CREATED.value());
        return responseUploadJson.as(Artefact.class);

    }

    private Artefact uploadFlatFile() {
        Map<String, String> headerMapUploadFlatFile = new ConcurrentHashMap<>();
        headerMapUploadFlatFile.put(HttpHeaders.AUTHORIZATION, dataManagementAccessToken);
        headerMapUploadFlatFile.put("x-type", ARTEFACT_TYPE.toString());
        headerMapUploadFlatFile.put("x-provenance", PROVENANCE);
        headerMapUploadFlatFile.put("x-source-artefact-id", SOURCE_ARTEFACT_ID);
        headerMapUploadFlatFile.put("x-display-from", DISPLAY_FROM.toString());
        headerMapUploadFlatFile.put("x-display-to", DISPLAY_FROM.plusDays(1).toString());
        headerMapUploadFlatFile.put("x-court-id", LOCATION_ID);
        headerMapUploadFlatFile.put("x-list-type", ListType.ET_DAILY_LIST.toString());
        headerMapUploadFlatFile.put("x-content-date", CONTENT_DATE.toString());
        headerMapUploadFlatFile.put("x-sensitivity", "PUBLIC");
        headerMapUploadFlatFile.put("x-language", LANGUAGE.toString());
        headerMapUploadFlatFile.put("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE);

        String filePath = Thread.currentThread().getContextClassLoader()
            .getResource("data/testFlatFile.pdf").getPath();
        File pdfFile = new File(filePath);

        final Response responseUploadFlatFile = doDataManagementPostRequestMultiPart(
            PUBLICATION_URL,
            headerMapUploadFlatFile, pdfFile
        );

        AssertionsForClassTypes.assertThat(responseUploadFlatFile.getStatusCode()).isEqualTo(CREATED.value());
        return responseUploadFlatFile.as(Artefact.class);
    }

    private Notification extractNotification(Response response) throws NotificationClientException {
        assertThat(response.getStatusCode()).isEqualTo(ACCEPTED.value());

        String referenceId = response.getBody().asString();
        assertThat(referenceId)
            .isNotEmpty();

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

        Notification notification = notificationList.getNotifications().get(0);

        assertThat(notification.getEmailAddress())
            .as(EMAIL_ADDRESS_ERROR)
            .hasValue(TEST_EMAIL);

        return notification;
    }

    @BeforeAll
    public void setup() throws IOException {

        doDataManagementPostRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_ID,
            Map.of(AUTHORIZATION, dataManagementAccessToken), LOCATION_NAME
        );

        jsonArtefactId = uploadArtefact(
            LANGUAGE.toString(),
            LIST_TYPE.toString(),
            "data/sjpPressList.json"
        ).getArtefactId();
        jsonArtefactIdWelsh = uploadArtefact(
            "WELSH",
            ListType.CIVIL_DAILY_CAUSE_LIST.toString(),
            "data/civilDailyCauseList.json"
        ).getArtefactId();
        flatFileArtefactId = uploadFlatFile().getArtefactId();
    }

    @AfterAll
    public void teardown() {
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_PUBLICATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, dataManagementAccessToken)
        );
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, dataManagementAccessToken)
        );
    }

    @Test
    void shouldSendJsonUploadSubscriptionByLocationEmail() throws NotificationClientException {

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(TEST_EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID)));


        BulkSubscriptionEmail requestBody = new BulkSubscriptionEmail();
        requestBody.setArtefactId(jsonArtefactId);
        requestBody.setSubscriptionEmails(List.of(subscriptionEmail));

        final Response response = doPostRequest(
            BULK_SUBSCRIPTION_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue(LOCATION_NAME + EMAIL_SUBJECT_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(EMAIL_BODY);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(DOWNLOAD_PDF_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Download the case list as an Excel spreadsheet.");
    }

    @Test
    void shouldSendJsonUploadSubscriptionByLocationEmailWelsh() throws NotificationClientException {

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(TEST_EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID)));


        BulkSubscriptionEmail requestBody = new BulkSubscriptionEmail();
        requestBody.setArtefactId(jsonArtefactIdWelsh);
        requestBody.setSubscriptionEmails(List.of(subscriptionEmail));

        final Response response = doPostRequest(
            BULK_SUBSCRIPTION_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue(LOCATION_NAME + EMAIL_SUBJECT_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(EMAIL_BODY);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(DOWNLOAD_PDF_TEXT);
    }

    @Test
    void shouldSendFlatFileUploadSubscriptionByLocationEmail() throws NotificationClientException {

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(TEST_EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID)));


        BulkSubscriptionEmail requestBody = new BulkSubscriptionEmail();
        requestBody.setArtefactId(flatFileArtefactId);
        requestBody.setSubscriptionEmails(List.of(subscriptionEmail));

        final Response response = doPostRequest(
            BULK_SUBSCRIPTION_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue(LOCATION_NAME + EMAIL_SUBJECT_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(EMAIL_BODY);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Download the document.");
    }

    @Test
    void shouldSendJsonUploadSubscriptionByCaseNumber() throws NotificationClientException {

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(TEST_EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.CASE_NUMBER, List.of(TEST_CASE_NUMBER)));


        BulkSubscriptionEmail requestBody = new BulkSubscriptionEmail();
        requestBody.setArtefactId(jsonArtefactIdWelsh);
        requestBody.setSubscriptionEmails(List.of(subscriptionEmail));

        final Response response = doPostRequest(
            BULK_SUBSCRIPTION_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("With case number or ID " + TEST_CASE_NUMBER
                          + " (TestCaseName) " + LOCATION_NAME + EMAIL_SUBJECT_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(EMAIL_BODY);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(DOWNLOAD_PDF_TEXT);
    }

    @Test
    void shouldSendJsonUploadSubscriptionByCaseUrn() throws NotificationClientException {

        SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
        subscriptionEmail.setEmail(TEST_EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.CASE_URN, List.of(TEST_CASE_URN)));

        BulkSubscriptionEmail requestBody = new BulkSubscriptionEmail();
        requestBody.setArtefactId(jsonArtefactId);
        requestBody.setSubscriptionEmails(List.of(subscriptionEmail));

        final Response response = doPostRequest(
            BULK_SUBSCRIPTION_URL,
            Map.of(AUTHORIZATION, bearerToken),
            requestBody
        );

        Notification notification = extractNotification(response);

        assertThat(notification.getSubject())
            .as(EMAIL_SUBJECT_ERROR)
            .hasValue("With unique reference number " + TEST_CASE_URN + " "
                          + LOCATION_NAME + EMAIL_SUBJECT_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_NAME_ERROR)
            .contains(EMAIL_BODY);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains(DOWNLOAD_PDF_TEXT);

        assertThat(notification.getBody())
            .as(EMAIL_BODY_ERROR)
            .contains("Download the case list as an Excel spreadsheet.");
    }
}
