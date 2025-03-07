package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.core.IsNull;
import org.jose4j.base64url.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.PublicationMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.utils.IntegrationTestBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;
import static uk.gov.hmcts.reform.pip.model.publication.ArtefactType.LIST;
import static uk.gov.hmcts.reform.pip.model.publication.Language.BI_LINGUAL;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.FAMILY_DAILY_CAUSE_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.Sensitivity.PUBLIC;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_ID;

@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods",
    "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
class NotifyTest extends IntegrationTestBase {
    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING = """
        {
            "email": "test@email.com",
            "isExisting": "true",
            "fullName": "fullName"
        }
        """;

    private static final String VALID_WELCOME_REQUEST_BODY_NEW = """
        {
            "email": "test@email.com",
            "isExisting": "false",
            "fullName": "fullName"
        }
        """;

    private static final String VALID_ADMIN_CREATION_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "surname": "surname",
            "forename": "forename"
        };
        """;

    private static final String INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "isExisting":
        }
        """;

    private static final String VALID_DUPLICATE_MEDIA_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "fullName": "fullName"
        };
        """;

    private static final String DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "fullName":
        }
        """;

    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY = """
        {
            "locationName": "Test Location",
            "subscriberEmails": [
                "test.system.admin@justice.gov.uk"
            ]
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL = """
        {
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_RESULT = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_TYPE = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL_LIST = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "detailString": "test"
        }
        """;

    private static final String VALID_MEDIA_VERIFICATION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@email.com"
        }
        """;

    private static final String VALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String INVALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": "invalid"
        }
         """;

    private static final String INVALID_NOTIFY_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY = """
        {
            "email": "test@test.com",
            "fullName": "testName",
            "lastSignedInDate": "01 May 2022"
        }
        """;

    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";
    private static final String MEDIA_REPORTING_EMAIL_URL = "/notify/media/report";
    private static final String MI_REPORTING_EMAIL_URL = "/notify/mi/report";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = "/notify/media/verification";
    private static final String MEDIA_REJECTION_EMAIL_URL = "/notify/media/reject";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = "/notify/user/sign-in";
    private static final String NOTIFY_SYSTEM_ADMIN_URL = "/notify/sysadmin/update";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL = "/notify/location-subscription-delete";
    private static final String DUPLICATE_MEDIA_EMAIL_URL = "/notify/duplicate/media";

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final Channel EMAIL_CHANNEL = Channel.EMAIL;
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_username";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.role";
    private static final String INVALID_CONTENT = "invalid content";
    private static final LocalDateTime CREATED_DATE = LocalDateTime.of(2022, 1, 19, 13, 45, 50);
    private static final String CREATED_DATE_STRING = "2022-01-19 13:45:50";
    private static final LocalDateTime LAST_SIGNED_IN = LocalDateTime.of(2023,1, 25, 14, 22, 43);
    private static final SearchType SEARCH_TYPE = CASE_ID;
    private static final String SEARCH_VALUE = "1234";
    private static final String LOCATION_NAME = "Location";
    public static final UUID ARTEFACT_ID = UUID.randomUUID();
    public static final LocalDateTime DISPLAY_FROM = LocalDateTime.of(2022, 1, 19, 13, 45, 50);
    public static final LocalDateTime DISPLAY_TO = LocalDateTime.of(2025,1, 19, 13, 45, 50);
    public static final String MANUAL_UPLOAD_PROVENANCE = "MANUAL_UPLOAD";
    public static final String SOURCE_ARTEFACT_ID = "1234";
    public static final Integer SUPERSEDED_COUNT = 0;
    public static final LocalDateTime CONTENT_DATE = LocalDateTime.of(2025,1, 19, 13, 45);
    private static final String FILE_PERSONALISATION = "link_to_file";
    private static final String FILE_NAME_PERSONALISATION = "file";

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME
        ));

    private static final AccountMiData ACCOUNT_MI_RECORD = new AccountMiData(USER_ID, ID.toString(), PI_AAD,
                                                                             INTERNAL_ADMIN_CTSC,
                                                                             CREATED_DATE, LAST_SIGNED_IN);
    private static final AllSubscriptionMiData ALL_SUBS_MI_RECORD = new AllSubscriptionMiData(
        USER_ID, EMAIL_CHANNEL, SEARCH_TYPE, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final LocationSubscriptionMiData LOCAL_SUBS_MI_RECORD = new LocationSubscriptionMiData(
        USER_ID, SEARCH_VALUE, EMAIL_CHANNEL, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final PublicationMiData PUBLICATION_MI_RECORD = new PublicationMiData(
        ARTEFACT_ID, DISPLAY_FROM, DISPLAY_TO, BI_LINGUAL, MANUAL_UPLOAD_PROVENANCE, PUBLIC, SOURCE_ARTEFACT_ID,
        SUPERSEDED_COUNT, LIST, CONTENT_DATE, "3", FAMILY_DAILY_CAUSE_LIST);

    private static final PublicationMiData PUBLICATION_MI_RECORD_WITHOUT_LOCATION_NAME = new PublicationMiData(
        ARTEFACT_ID, DISPLAY_FROM, DISPLAY_TO, BI_LINGUAL, MANUAL_UPLOAD_PROVENANCE, PUBLIC, SOURCE_ARTEFACT_ID,
        SUPERSEDED_COUNT, LIST, CONTENT_DATE, "NoMatch4", FAMILY_DAILY_CAUSE_LIST);

    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();

    private static final List<AccountMiData> ACCOUNT_MI_DATA = List.of(ACCOUNT_MI_RECORD, ACCOUNT_MI_RECORD);
    private static final List<AllSubscriptionMiData> ALL_SUBS_MI_DATA = List.of(ALL_SUBS_MI_RECORD, ALL_SUBS_MI_RECORD);
    private static final List<LocationSubscriptionMiData> LOCAL_SUBS_MI_DATA = List.of(LOCAL_SUBS_MI_RECORD,
                                                                                       LOCAL_SUBS_MI_RECORD);

    private static final String[] EXPECTED_PUBLICATION_HEADERS = {"artefact_id", "display_from",
        "display_to", "language", "provenance", "sensitivity", "source_artefact_id", "superseded_count", "type",
        "content_date", "court_id", "court_name", "list_type"};

    private static final String[] EXPECTED_ACCOUNT_HEADERS = {"user_id", "provenance_user_id",
        "user_provenance", "roles", "created_date", "last_signed_in_date"};

    private static final String[] EXPECTED_ALL_SUBSCRIPTION_HEADERS =  {"id", "channel",
        "search_type", "user_id", "court_name", "created_date"};

    private static final String[] EXPECTED_LOCATION_SUBSCRIPTION_HEADERS = {"id", "search_value",
        "channel", "user_id", "court_name", "created_date"};

    private static List<PublicationMiData> publicationMiData;

    private String validMediaReportingJson;
    private String validLocationsListJson;

    @Autowired
    private MockMvc mockMvc;
    private WebClient webClient;
    @Autowired
    private EmailClient emailClient;

    @BeforeAll
    public static void setupAll() {
        PUBLICATION_MI_RECORD.setLocationName(LOCATION_NAME);

        publicationMiData = List.of(PUBLICATION_MI_RECORD, PUBLICATION_MI_RECORD_WITHOUT_LOCATION_NAME);
    }

    @BeforeEach
    void setup() throws IOException {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(
            UUID.randomUUID(),
            "TEST",
            "1234"
        ));

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

        validMediaReportingJson = ow.writeValueAsString(MEDIA_APPLICATION_LIST);
        validLocationsListJson = ow.writeValueAsString(NO_MATCH_ARTEFACT_LIST);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private Workbook setupMiEndpointWithData() throws Exception {
        when(dataManagementService.getMiData()).thenReturn(publicationMiData);
        when(accountManagementService.getAccountMiData()).thenReturn(ACCOUNT_MI_DATA);
        when(accountManagementService.getAllSubscriptionMiData()).thenReturn(ALL_SUBS_MI_DATA);
        when(accountManagementService.getLocationSubscriptionMiData()).thenReturn(LOCAL_SUBS_MI_DATA);

        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        byte[] file = Base64.decode(((JSONObject) personalisationCapture.getValue().get(FILE_PERSONALISATION))
                                        .get(FILE_NAME_PERSONALISATION).toString());

        ByteArrayInputStream outputFile = new ByteArrayInputStream(file);
        return new XSSFWorkbook(outputFile);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private Workbook setupMiEndpointNoData() throws Exception {
        when(dataManagementService.getMiData()).thenReturn(List.of());
        when(accountManagementService.getAccountMiData()).thenReturn(List.of());
        when(accountManagementService.getAllSubscriptionMiData()).thenReturn(List.of());
        when(accountManagementService.getLocationSubscriptionMiData()).thenReturn(List.of());

        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));

        byte[] file = Base64.decode(((JSONObject) personalisationCapture.getValue().get(FILE_PERSONALISATION))
                                        .get(FILE_NAME_PERSONALISATION).toString());

        ByteArrayInputStream outputFile = new ByteArrayInputStream(file);
        return new XSSFWorkbook(outputFile);
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testValidPayloadReturnsSuccessNew() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendDuplicateMediaAccountEmailBadRequest() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendAdminAccountWelcomeEmailBadRequest() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendMediaReportingEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendUnidentifiedBlobEmailBadRequest() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testValidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaUserVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMediaUserRejectionEmail() throws Exception {

        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendMediaUserRejectionEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(INVALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testSendMediaUserRejectionEmailUnauthorized() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsersBadRequest() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMiReportingEmailForPublications() throws Exception {
        try (Workbook workbook = setupMiEndpointWithData()) {

            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);

            assertThat(workbook.getSheet("Publications")).isNotNull();

            Sheet publicationsSheet = workbook.getSheet("Publications");

            assertThat(publicationsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_PUBLICATION_HEADERS);

            assertThat(publicationsSheet.getRow(1))
                .extracting(Cell::getStringCellValue)
                .containsExactly(ARTEFACT_ID.toString(),
                                 CREATED_DATE_STRING,
                                 "2025-01-19 13:45:50",
                                 BI_LINGUAL.toString(),
                                 MANUAL_UPLOAD_PROVENANCE,
                                 PUBLIC.toString(),
                                 SOURCE_ARTEFACT_ID,
                                 SUPERSEDED_COUNT.toString(),
                                 LIST.toString(),
                                 "2025-01-19 13:45:00",
                                 "3",
                                 LOCATION_NAME,
                                 FAMILY_DAILY_CAUSE_LIST.toString()
                );

            assertThat(publicationsSheet.getRow(2))
                .extracting(Cell::getStringCellValue)
                .containsExactly(ARTEFACT_ID.toString(), CREATED_DATE_STRING, "2025-01-19 13:45:50",
                                 BI_LINGUAL.toString(), MANUAL_UPLOAD_PROVENANCE, PUBLIC.toString(), SOURCE_ARTEFACT_ID,
                                 SUPERSEDED_COUNT.toString(), LIST.toString(), "2025-01-19 13:45:00", "NoMatch4", "",
                                 FAMILY_DAILY_CAUSE_LIST.toString()
                );
        }
    }

    @Test
    void testSendMiReportingEmailForPublicationsWhenNoData() throws Exception {
        try (Workbook workbook = setupMiEndpointNoData()) {

            Sheet publicationsSheet = workbook.getSheet("Publications");

            assertThat(publicationsSheet.getLastRowNum()).isEqualTo(0);

            assertThat(publicationsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_PUBLICATION_HEADERS);
        }
    }

    @Test
    void testSendMiReportingEmailForAccounts() throws Exception {
        try (Workbook workbook = setupMiEndpointWithData()) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);

            assertThat(workbook.getSheet("User accounts")).isNotNull();

            Sheet userAccountSheet = workbook.getSheet("User accounts");

            assertThat(userAccountSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_ACCOUNT_HEADERS);

            assertThat(userAccountSheet.getRow(1))
                .extracting(Cell::getStringCellValue)
                .containsExactly(USER_ID.toString(), ID.toString(), PI_AAD.toString(), INTERNAL_ADMIN_CTSC.toString(),
                                 CREATED_DATE_STRING, "2023-01-25 14:22:43"
                );
        }
    }

    @Test
    void testSendMiReportingEmailForAccountsWhenNoData() throws Exception {
        try (Workbook workbook = setupMiEndpointNoData()) {

            Sheet userAccountsSheet = workbook.getSheet("User accounts");

            assertThat(userAccountsSheet.getLastRowNum()).isEqualTo(0);

            assertThat(userAccountsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_ACCOUNT_HEADERS);
        }
    }

    @Test
    void testSendMiReportingEmailForAllSubscriptions() throws Exception {
        try (Workbook workbook = setupMiEndpointWithData()) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);

            Sheet allSubscriptionsSheet = workbook.getSheet("All subscriptions");

            assertThat(allSubscriptionsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_ALL_SUBSCRIPTION_HEADERS);

            assertThat(allSubscriptionsSheet.getRow(1))
                .extracting(Cell::getStringCellValue)
                .containsExactly(USER_ID.toString(), EMAIL_CHANNEL.toString(), SEARCH_TYPE.toString(), ID.toString(),
                                 LOCATION_NAME, CREATED_DATE_STRING
                );
        }
    }

    @Test
    void testSendMiReportingEmailForAllSubscriptionsWhenNoData() throws Exception {
        try (Workbook workbook = setupMiEndpointNoData()) {

            Sheet allSubscriptionsSheet = workbook.getSheet("All subscriptions");

            assertThat(allSubscriptionsSheet.getLastRowNum()).isEqualTo(0);

            assertThat(allSubscriptionsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_ALL_SUBSCRIPTION_HEADERS);
        }
    }

    @Test
    void testSendMiReportingEmailForLocationSubscriptions() throws Exception {
        try (Workbook workbook = setupMiEndpointWithData()) {

            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);

            assertThat(workbook.getSheet("Location subscriptions")).isNotNull();

            Sheet locationSubscriptionsSheet = workbook.getSheet("Location subscriptions");

            assertThat(locationSubscriptionsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_LOCATION_SUBSCRIPTION_HEADERS);

            assertThat(locationSubscriptionsSheet.getRow(1))
                .extracting(Cell::getStringCellValue)
                .containsExactly(USER_ID.toString(), SEARCH_VALUE, EMAIL_CHANNEL.toString(), ID.toString(),
                                 LOCATION_NAME, CREATED_DATE_STRING
                );
        }
    }

    @Test
    void testSendMiReportingEmailForLocationSubscriptionsWhenNoData() throws Exception {
        try (Workbook workbook = setupMiEndpointNoData()) {

            Sheet locationSubscriptionsSheet = workbook.getSheet("Location subscriptions");

            assertThat(locationSubscriptionsSheet.getLastRowNum()).isEqualTo(0);

            assertThat(locationSubscriptionsSheet.getRow(0))
                .extracting(Cell::getStringCellValue)
                .containsExactly(EXPECTED_LOCATION_SUBSCRIPTION_HEADERS);
        }
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMiReportingEmail() throws Exception {
        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendSystemAdminMissingRequesterEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingActionResult() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_RESULT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingChangeType() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_TYPE)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingEmailList() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL_LIST)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminBadPayload() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content("content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailBadRequest() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
