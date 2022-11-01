package uk.gov.hmcts.reform.pip.publication.services.service;

import com.openhtmltopdf.util.XRRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("PMD.ExcessiveImports")
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

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST = List.of(new MediaApplication(
        UUID.randomUUID(), "Test user", "test@email.com", "Test employer",
        UUID.randomUUID().toString(), "test-image.png", LocalDateTime.now(),
        "REJECTED", LocalDateTime.now()
    ));

    public static final String TEST_STRING = "test";
    public static final String ONE_TWO_THREE_FOUR = "1234";
    private static final byte[] TEST_BYTE = "Test byte".getBytes();

    private static final Map<String, ListType> LIST_TYPE_LOOKUP = Map.of(
        "civilAndFamilyDailyCauseList.json", ListType.CIVIL_DAILY_CAUSE_LIST,
        "civilDailyCauseList.json", ListType.CIVIL_DAILY_CAUSE_LIST,
        "copDailyCauseList.json", ListType.COP_DAILY_CAUSE_LIST,
        "familyDailyCauseList.json", ListType.FAMILY_DAILY_CAUSE_LIST,
        "sjpPressMockJul22.json", ListType.SJP_PRESS_LIST,
        "sjpPublicList.json", ListType.SJP_PUBLIC_LIST,
        "sscsDailyList.json", ListType.SSCS_DAILY_LIST,
        "etFortnightlyPressList.json", ListType.ET_FORTNIGHTLY_PRESS_LIST
    );

    private static Location location = new Location();

    private String getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        }
    }

    private Artefact preBuiltArtefact(ListType listType) {
        Artefact artefact = new Artefact();
        artefact.setContentDate(LocalDateTime.now());
        artefact.setListType(listType);
        artefact.setProvenance(TEST_STRING);
        artefact.setLanguage(Language.ENGLISH);
        artefact.setLocationId(ONE_TWO_THREE_FOUR);
        return artefact;
    }

    @BeforeAll
    public static void setup()  {
        location.setName(TEST_STRING);
        location.setRegion(Collections.singletonList(TEST_STRING));
    }

    @ParameterizedTest
    @ValueSource(strings = {"civilAndFamilyDailyCauseList.json", "civilDailyCauseList.json",
        "copDailyCauseList.json", "familyDailyCauseList.json", "sjpPressMockJul22.json", "sjpPublicList.json",
        "sscsDailyList.json", "etFortnightlyPressList.json"})
    void testAllPdfListsAccessible(String filePath) throws IOException {
        ListType listType = LIST_TYPE_LOOKUP.get(filePath);
        UUID uuid = UUID.randomUUID();
        Artefact artefact = preBuiltArtefact(listType);
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn(getInput("/mocks/" + filePath));
        when(dataManagementService.getArtefact(uuid)).thenReturn(artefact);
        when(dataManagementService.getLocation(ONE_TWO_THREE_FOUR)).thenReturn(location);
        String htmlOutput = fileCreationService.jsonToHtml(uuid);
        byte[] outputPdf = fileCreationService.generatePdfFromHtml(htmlOutput, true);
        assertNotNull(outputPdf, "Output PDF is not a valid file. Did the generation process work?");
    }

    @ParameterizedTest
    @ValueSource(strings = {"civilAndFamilyDailyCauseList.json", "civilDailyCauseList.json",
        "copDailyCauseList.json", "familyDailyCauseList.json", "sjpPressMockJul22.json", "sjpPublicList.json",
        "sscsDailyList.json", "etFortnightlyPressList.json"})
    void testAllPdfListsNonAccessible(String filePath) throws IOException {
        ListType listType = LIST_TYPE_LOOKUP.get(filePath);
        UUID uuid = UUID.randomUUID();
        Artefact artefact = preBuiltArtefact(listType);
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn(getInput("/mocks/" + filePath));
        when(dataManagementService.getArtefact(uuid)).thenReturn(artefact);
        when(dataManagementService.getLocation(ONE_TWO_THREE_FOUR)).thenReturn(location);
        String htmlOutput = fileCreationService.jsonToHtml(uuid);
        byte[] outputPdf = fileCreationService.generatePdfFromHtml(htmlOutput, false);
        assertNotNull(outputPdf, "Output PDF is not a valid file. Did the generation process work?");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPdfGenerationSuccess(boolean accessible) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "testThyme.html")), writer,
                     Charset.defaultCharset()
        );

        byte[] outputPdf = fileCreationService.generatePdfFromHtml(writer.toString(), accessible);

        try (PDDocument doc = PDDocument.load(outputPdf)) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            String outputText = pdfTextStripper.getText(doc);

            assertTrue(
                outputText.contains(
                    "An example file for the creation of PDF lists from our JSON artefact"),
                "Output pdf does not contain input text"
            );

            assertEquals(doc.getNumberOfPages(), 5, "Output pdf is not the correct length");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPdfGenerationFailure(boolean accessibility) {
        String badInputHtml = "1 2 3 4 broken html";
        assertThrows(XRRuntimeException.class, () ->
            fileCreationService.generatePdfFromHtml(badInputHtml, accessibility), "Exception not thrown");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testJsontoHtmltoPdf(boolean accessibility) throws IOException {
        Artefact artefact = new Artefact();
        artefact.setContentDate(LocalDateTime.now());
        artefact.setLocationId("1");
        artefact.setProvenance("france");
        artefact.setLanguage(Language.ENGLISH);
        artefact.setListType(ListType.MAGISTRATES_STANDARD_LIST);
        UUID uuid = UUID.randomUUID();
        String inputJson = "{\"document\":{\"value1\":\"x\",\"value2\":\"hiddenTestString\"}}";
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn(inputJson);
        when(dataManagementService.getArtefact(uuid)).thenReturn(artefact);
        when(dataManagementService.getLocation("1")).thenReturn(location);

        byte[] outputPdf = fileCreationService.generatePdfFromHtml(fileCreationService.jsonToHtml(uuid), accessibility);
        try (PDDocument doc = PDDocument.load(outputPdf)) {
            assertEquals(doc.getNumberOfPages(), 1, "pages not correct");
            PDFTextStripper stripper = new PDFTextStripper();
            assertTrue(stripper.getText(doc).contains("hiddenTestString"), "hidden text non-existent in final file");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Test
    void testGenerateExcelSpreadsheet() throws IOException {
        Artefact artefact = new Artefact();
        artefact.setListType(ListType.SJP_PUBLIC_LIST);
        UUID uuid = UUID.randomUUID();
        when(dataManagementService.getArtefactJsonBlob(uuid))
            .thenReturn(getInput("/mocks/sjpPublicList.json"));
        when(dataManagementService.getArtefact(uuid)).thenReturn(artefact);

        byte[] outputExcel = fileCreationService.generateExcelSpreadsheet(uuid);

        assertNotNull(outputExcel, "Returned result was empty");
    }

    @Test
    void testCreateMediaApplicationReportingCsvSuccess() {
        assertNotNull(
            fileCreationService.createMediaApplicationReportingCsv(MEDIA_APPLICATION_LIST),
            "Csv creation should not return null"
        );
    }

    @Test
    void testGenerateMiReportSuccess() throws IOException {
        String data = "1,2,3";
        when(dataManagementService.getMiData()).thenReturn(data);
        when(accountManagementService.getMiData()).thenReturn(data);
        when(subscriptionManagementService.getAllMiData()).thenReturn(data);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(data);
        when(excelGenerationService.generateMultiSheetWorkBook(any())).thenReturn(TEST_BYTE);

        assertThat(fileCreationService.generateMiReport()).isEqualTo(TEST_BYTE);
    }
}
