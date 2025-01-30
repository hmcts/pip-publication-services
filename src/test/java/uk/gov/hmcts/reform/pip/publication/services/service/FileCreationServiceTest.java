package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.PublicationMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;
import static uk.gov.hmcts.reform.pip.model.publication.ArtefactType.LIST;
import static uk.gov.hmcts.reform.pip.model.publication.Language.BI_LINGUAL;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.FAMILY_DAILY_CAUSE_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.Sensitivity.PUBLIC;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_ID;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CouplingBetweenObjects")
class FileCreationServiceTest {

    @Mock
    private DataManagementService dataManagementService;

    @Mock
    private AccountManagementService accountManagementService;

    @Mock
    private SubscriptionManagementService subscriptionManagementService;

    @Mock
    private ExcelGenerationService excelGenerationService;

    @InjectMocks
    private FileCreationService fileCreationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final byte[] TEST_BYTE = "Test byte".getBytes();
    private static final LocalDateTime REQUEST_DATE = LocalDateTime.now();
    private static final LocalDateTime STATUS_DATE = LocalDateTime.now();

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST = List.of(new MediaApplication(
        UUID.randomUUID(), "Test user", "test@email.com", "Test employer",
        UUID.randomUUID().toString(), "test-image.png", REQUEST_DATE,
        "REJECTED", STATUS_DATE
    ));
    private static final String EXPECTED_HEADER = "\"Full name\",\"Email\",\"Employer\",\"Request date\","
        + "\"Status\",\"Status date\"";
    private static final String EXPECTED_CONTENT = "\"Test user\",\"test@email.com\",\"Test employer\",\""
        + REQUEST_DATE.format(DATE_TIME_FORMATTER) + "\",\"REJECTED\",\""
        + STATUS_DATE.format(DATE_TIME_FORMATTER) + "\"";

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ID = "1234";
    private static final LocalDateTime CREATED_DATE = LocalDateTime.of(2022, 1, 19, 13, 45, 50);
    private static final LocalDateTime LAST_SIGNED_IN = LocalDateTime.of(2023,1, 25, 14, 22, 43);
    private static final String CREATED_DATE_STRING = "2022-01-19 13:45:50";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final SearchType SEARCH_TYPE = CASE_ID;
    private static final String SEARCH_VALUE = "193254";
    private static final String LOCATION_NAME = "Location";
    public static final UUID ARTEFACT_ID = UUID.randomUUID();
    public static final LocalDateTime DISPLAY_FROM = LocalDateTime.of(2022, 1, 19, 13, 45, 50);
    public static final LocalDateTime DISPLAY_TO = LocalDateTime.of(2025,1, 19, 13, 45, 50);
    public static final String MANUAL_UPLOAD_PROVENANCE = "MANUAL_UPLOAD";
    public static final String SOURCE_ARTEFACT_ID = "1234";
    public static final Integer SUPERSEDED_COUNT = 0;
    public static final LocalDateTime CONTENT_DATE = LocalDateTime.of(2024,1, 19, 13, 45);

    private static final AccountMiData ACCOUNT_MI_RECORD = new AccountMiData(USER_ID, ID, PI_AAD, INTERNAL_ADMIN_CTSC,
                                                                     CREATED_DATE, LAST_SIGNED_IN);
    private static final AllSubscriptionMiData ALL_SUBS_MI_RECORD = new AllSubscriptionMiData(
        USER_ID, EMAIL, SEARCH_TYPE, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final LocationSubscriptionMiData LOCAL_SUBS_MI_RECORD = new LocationSubscriptionMiData(
        USER_ID, SEARCH_VALUE, EMAIL, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final PublicationMiData PUBLICATION_MI_RECORD = new PublicationMiData(
        ARTEFACT_ID, DISPLAY_FROM, DISPLAY_TO, BI_LINGUAL, MANUAL_UPLOAD_PROVENANCE, PUBLIC, SOURCE_ARTEFACT_ID,
        SUPERSEDED_COUNT, LIST, CONTENT_DATE, "3", FAMILY_DAILY_CAUSE_LIST);

    private static final PublicationMiData PUBLICATION_MI_RECORD_WITHOUT_LOCATION_NAME = new PublicationMiData(
        ARTEFACT_ID, DISPLAY_FROM, DISPLAY_TO, BI_LINGUAL, MANUAL_UPLOAD_PROVENANCE, PUBLIC, SOURCE_ARTEFACT_ID,
        SUPERSEDED_COUNT, LIST, CONTENT_DATE, "NoMatch4", FAMILY_DAILY_CAUSE_LIST);

    private static final List<AccountMiData> ACCOUNT_MI_DATA = List.of(ACCOUNT_MI_RECORD, ACCOUNT_MI_RECORD);
    private static final List<AllSubscriptionMiData> ALL_SUBS_MI_DATA = List.of(ALL_SUBS_MI_RECORD, ALL_SUBS_MI_RECORD);
    private static final List<LocationSubscriptionMiData> LOCAL_SUBS_MI_DATA = List.of(LOCAL_SUBS_MI_RECORD,
                                                                                    LOCAL_SUBS_MI_RECORD);
    private static final String PUBLICATION_MI_DATA_KEY = "Publications";
    private static final String ACCOUNT_MI_DATA_KEY = "User accounts";
    private static final String ALL_SUBSCRIPTION_MI_DATA_KEY = "All subscriptions";
    private static final String LOCATION_SUBSCRIPTION_MI_DATA_KEY = "Location subscriptions";
    private static final String MI_DATA_MATCH_MESSAGE = "MI data does not match";

    private static List<PublicationMiData> publicationMiData;

    @BeforeAll
    public static void setup() {
        PUBLICATION_MI_RECORD.setLocationName(LOCATION_NAME);

        publicationMiData = List.of(PUBLICATION_MI_RECORD, PUBLICATION_MI_RECORD_WITHOUT_LOCATION_NAME);
    }


    @Test
    void testCreateMediaApplicationReportingCsvSuccess() {
        byte[] result = fileCreationService.createMediaApplicationReportingCsv(MEDIA_APPLICATION_LIST);
        String[] csvResult = new String(result, StandardCharsets.UTF_8).split(System.lineSeparator());

        assertThat(csvResult)
            .as("CSV line count does not match")
            .hasSize(2);
        assertThat(csvResult[0])
            .as("CSV header does not match")
            .isEqualTo(EXPECTED_HEADER);
        assertThat(csvResult[1])
            .as("CSV content does not match")
            .isEqualTo(EXPECTED_CONTENT);
    }

    @Test
    void testGenerateMiReportSuccess() throws IOException {
        when(dataManagementService.getMiData()).thenReturn(publicationMiData);
        when(accountManagementService.getMiData()).thenReturn(ACCOUNT_MI_DATA);
        when(subscriptionManagementService.getAllMiData()).thenReturn(ALL_SUBS_MI_DATA);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(LOCAL_SUBS_MI_DATA);
        when(excelGenerationService.generateMultiSheetWorkBook(any())).thenReturn(TEST_BYTE);

        assertThat(fileCreationService.generateMiReport()).isEqualTo(TEST_BYTE);
    }

    @Test
    void testExtractMiData() {
        when(dataManagementService.getMiData()).thenReturn(publicationMiData);
        when(accountManagementService.getMiData()).thenReturn(ACCOUNT_MI_DATA);
        when(subscriptionManagementService.getAllMiData()).thenReturn(ALL_SUBS_MI_DATA);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(LOCAL_SUBS_MI_DATA);

        Map<String, List<String[]>> results = fileCreationService.extractMiData();

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(PUBLICATION_MI_DATA_KEY);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(ACCOUNT_MI_DATA_KEY);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(ALL_SUBSCRIPTION_MI_DATA_KEY);

        assertThat(results)
            .as(MI_DATA_MATCH_MESSAGE)
            .containsKey(LOCATION_SUBSCRIPTION_MI_DATA_KEY);

        List<String[]> publicationMiData = results.get(PUBLICATION_MI_DATA_KEY);
        assertThat(publicationMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(3)
            .contains(
                new String[]{"artefact_id", "display_from", "display_to", "language", "provenance", "sensitivity",
                             "source_artefact_id", "superseded_count", "type", "content_date", "court_id",
                             "court_name", "list_type"},
                new String[]{ARTEFACT_ID.toString(), CREATED_DATE_STRING, "2025-01-19 13:45:50",
                    BI_LINGUAL.toString(), MANUAL_UPLOAD_PROVENANCE, PUBLIC.toString(), SOURCE_ARTEFACT_ID,
                    SUPERSEDED_COUNT.toString(), LIST.toString(), "2024-01-19 13:45:00", "3", LOCATION_NAME,
                    FAMILY_DAILY_CAUSE_LIST.toString() },
                new String[]{ARTEFACT_ID.toString(), CREATED_DATE_STRING, "2025-01-19 13:45:50",
                    BI_LINGUAL.toString(), MANUAL_UPLOAD_PROVENANCE, PUBLIC.toString(), SOURCE_ARTEFACT_ID,
                    SUPERSEDED_COUNT.toString(), LIST.toString(), "2024-01-19 13:45:00", "NoMatch4", "",
                    FAMILY_DAILY_CAUSE_LIST.toString() }
            );

        List<String[]> accountMiData = results.get(ACCOUNT_MI_DATA_KEY);
        assertThat(accountMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(3)
            .contains(
                new String[]{"user_id", "provenance_user_id", "user_provenance", "roles",
                    "created_date", "last_signed_in_date"},
                new String[]{USER_ID.toString(), ID, PI_AAD.toString(), INTERNAL_ADMIN_CTSC.toString(),
                    CREATED_DATE_STRING, "2023-01-25 14:22:43"}
            );

        List<String[]> allSubscriptionMiData = results.get(ALL_SUBSCRIPTION_MI_DATA_KEY);
        assertThat(allSubscriptionMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(3)
            .contains(
                new String[]{"id", "channel", "search_type", "user_id", "court_name", "created_date"},
                new String[]{USER_ID.toString(), EMAIL.toString(), SEARCH_TYPE.toString(), ID,
                    LOCATION_NAME, CREATED_DATE_STRING}
            );

        List<String[]> locationSubscriptionMiData = results.get(LOCATION_SUBSCRIPTION_MI_DATA_KEY);
        assertThat(locationSubscriptionMiData)
            .as(MI_DATA_MATCH_MESSAGE)
            .hasSize(3)
            .contains(
                new String[]{"id", "search_value", "channel", "user_id", "court_name", "created_date"},
                new String[]{USER_ID.toString(), SEARCH_VALUE, EMAIL.toString(), ID,
                    LOCATION_NAME, CREATED_DATE_STRING}
            );
    }
}
