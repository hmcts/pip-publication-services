package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocalSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.PublicationMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

@Slf4j
@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class FileCreationServiceTest extends RedisConfigurationTestBase {

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
    public static final LocalDateTime CONTENT_DATE = LocalDateTime.now();
    private static final String LOCATION_NAME_WITH_ID_3 = "Oxford Combined Court Centre";

    private static final AccountMiData ACCOUNT_MI_RECORD = new AccountMiData(USER_ID, ID, PI_AAD, INTERNAL_ADMIN_CTSC,
                                                                     CREATED_DATE, LAST_SIGNED_IN);
    private static final AllSubscriptionMiData ALL_SUBS_MI_RECORD = new AllSubscriptionMiData(
        USER_ID, EMAIL, SEARCH_TYPE, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final LocalSubscriptionMiData LOCAL_SUBS_MI_RECORD = new LocalSubscriptionMiData(
        USER_ID, SEARCH_VALUE, EMAIL, ID, LOCATION_NAME, CREATED_DATE
    );
    private static final PublicationMiData PUBLICATION_MI_RECORD = new PublicationMiData(
        ARTEFACT_ID, DISPLAY_FROM, DISPLAY_TO, BI_LINGUAL, MANUAL_UPLOAD_PROVENANCE, PUBLIC, SOURCE_ARTEFACT_ID,
        SUPERSEDED_COUNT, LIST, CONTENT_DATE, "3", LOCATION_NAME_WITH_ID_3, FAMILY_DAILY_CAUSE_LIST);

    private static final List<AccountMiData> ACCOUNT_MI_DATA = List.of(ACCOUNT_MI_RECORD, ACCOUNT_MI_RECORD);
    private static final List<AllSubscriptionMiData> ALL_SUBS_MI_DATA = List.of(ALL_SUBS_MI_RECORD, ALL_SUBS_MI_RECORD);
    private static final List<LocalSubscriptionMiData> LOCAL_SUBS_MI_DATA = List.of(LOCAL_SUBS_MI_RECORD,
                                                                                    LOCAL_SUBS_MI_RECORD);
    private static final List<PublicationMiData> PUBLICATION_MI_DATA = List.of(PUBLICATION_MI_RECORD,
                                                                               PUBLICATION_MI_RECORD);

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
        when(dataManagementService.getMiData()).thenReturn(PUBLICATION_MI_DATA);
        when(accountManagementService.getMiData()).thenReturn(ACCOUNT_MI_DATA);
        when(subscriptionManagementService.getAllMiData()).thenReturn(ALL_SUBS_MI_DATA);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(LOCAL_SUBS_MI_DATA);
        when(excelGenerationService.generateMultiSheetWorkBook(any())).thenReturn(TEST_BYTE);

        assertThat(fileCreationService.generateMiReport()).isEqualTo(TEST_BYTE);
    }
}
