package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class ExcelGenerationServiceTest {
    private static final String SHEET_COUNT_MESSAGE = "Incorrect number of sheets";
    private static final String ROW_COUNT_MESSAGE = "Incorrect number of rows";
    private static final String HEADER_ROW_MESSAGE = "Incorrect header row";

    private static final String SHEET_A = "Sheet A";
    private static final String SHEET_B = "Sheet B";
    private static final String SHEET_C = "Sheet C";

    private static final String COLUMN_A1 = "Column A1";
    private static final String COLUMN_A2 = "Column A2";
    private static final String COLUMN_A3 = "Column A3";

    private static final String COLUMN_B1 = "Column B1";
    private static final String COLUMN_B2 = "Column B2";
    private static final String COLUMN_B3 = "Column B3";
    private static final String COLUMN_B4 = "Column B4";

    private static final String COLUMN_C1 = "Column C1";
    private static final String COLUMN_C2 = "Column C2";

    private static final List<String[]> VALUE_A = Arrays.asList(
        new String[]{COLUMN_A1, COLUMN_A2, COLUMN_A3},
        new String[]{"1", "2", "3"},
        new String[]{"4", "5", "6"},
        new String[]{"7", "8", "9"}
    );

    private static final List<String[]> VALUE_B = Arrays.asList(
        new String[]{COLUMN_B1, COLUMN_B2, COLUMN_B3, COLUMN_B4},
        new String[]{"a", "b", "c", "d"},
        new String[]{"e", "f", "g", "h"}
    );

    private static final List<String[]> VALUE_C = Arrays.asList(
        new String[]{COLUMN_C1, COLUMN_C2},
        new String[]{"x", "y"}
    );

    private final ExcelGenerationService excelGenerationService = new ExcelGenerationService();

    @Test
    void testSuccessfulExcelGenerationForMultipleSheets() throws IOException {
        Map<String, List<String[]>> input = Map.of(
            SHEET_A, VALUE_A,
            SHEET_B, VALUE_B,
            SHEET_C, VALUE_C
        );

        byte[] result = excelGenerationService.generateMultiSheetWorkBook(input);
        ByteArrayInputStream file = new ByteArrayInputStream(result);
        Workbook workbook = new XSSFWorkbook(file);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(workbook.getNumberOfSheets())
            .as(SHEET_COUNT_MESSAGE)
            .isEqualTo(3);

        Sheet sheet = workbook.getSheet(SHEET_A);
        softly.assertThat(sheet.getRow(0))
            .as("Sheet A - " + HEADER_ROW_MESSAGE)
            .extracting(r -> r.getStringCellValue())
            .containsExactly(COLUMN_A1, COLUMN_A2, COLUMN_A3);
        softly.assertThat(sheet.getLastRowNum())
            .as("Sheet A - " + ROW_COUNT_MESSAGE)
            .isEqualTo(VALUE_A.size() - 1);

        sheet = workbook.getSheet(SHEET_B);
        softly.assertThat(sheet.getRow(0))
            .as("Sheet B - " + HEADER_ROW_MESSAGE)
            .extracting(r -> r.getStringCellValue())
            .containsExactly(COLUMN_B1, COLUMN_B2, COLUMN_B3, COLUMN_B4);
        softly.assertThat(sheet.getLastRowNum())
            .as("Sheet B - " + ROW_COUNT_MESSAGE)
            .isEqualTo(VALUE_B.size() - 1);

        sheet = workbook.getSheet(SHEET_C);
        softly.assertThat(sheet.getRow(0))
            .as("Sheet C - " + HEADER_ROW_MESSAGE)
            .extracting(r -> r.getStringCellValue())
            .containsExactly(COLUMN_C1, COLUMN_C2);
        softly.assertThat(sheet.getLastRowNum())
            .as("Sheet C - " + ROW_COUNT_MESSAGE)
            .isEqualTo(VALUE_C.size() - 1);

        softly.assertAll();
    }

    @Test
    void testSuccessfulExcelGenerationForASingleSheet() throws IOException {
        Map<String, List<String[]>> input = Map.of(SHEET_A, VALUE_A);

        byte[] result = excelGenerationService.generateMultiSheetWorkBook(input);
        ByteArrayInputStream file = new ByteArrayInputStream(result);
        Workbook workbook = new XSSFWorkbook(file);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(workbook.getNumberOfSheets())
            .as(SHEET_COUNT_MESSAGE)
            .isEqualTo(1);

        Sheet sheet = workbook.getSheetAt(0);
        softly.assertThat(sheet.getRow(0))
            .as(HEADER_ROW_MESSAGE)
            .extracting(r -> r.getStringCellValue())
            .containsExactly(COLUMN_A1, COLUMN_A2, COLUMN_A3);
        softly.assertThat(sheet.getLastRowNum())
            .as(ROW_COUNT_MESSAGE)
            .isEqualTo(VALUE_A.size() - 1);

        softly.assertAll();
    }
}
