package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
class SjpPressListConverterTest {

    @Autowired
    SjpPressListConverter sjpPressListConverter;

    private JsonNode getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            return new ObjectMapper().readTree(inputRaw);
        }
    }

    @Test
    void testSjpPressListTemplate() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("sjpPressList-language.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
        Map<String, String> metadataMap = Map.of("contentDate", Instant.now().toString(),
                                                 "provenance", "provenance",
                                                 "locationName", "location",
                                                 "language", "ENGLISH"
        );

        String outputHtml = sjpPressListConverter.convert(getInput("/mocks/sjpPressMockJul22.json"), metadataMap,
                                                          language);
        Document document = Jsoup.parse(outputHtml);
        assertThat(outputHtml).as("No html found").isNotEmpty();

        assertThat(document.title()).as("incorrect title found.")
            .isEqualTo("Single Justice Procedure - Press List "
                           + metadataMap.get("contentDate"));

        assertThat(document.getElementsByClass("mainHeaderText")
                       .select(".mainHeaderText > h1:nth-child(1)").text())
            .as("incorrect header text").isEqualTo("Single Justice Procedure - Press List");

        assertThat(document.select(
            "div.pageSeparatedCase:nth-child(2) > table:nth-child(3) > tbody:nth-child(1) >"
                + " tr:nth-child(7) > td:nth-child(2)").text())
            .as("incorrect value found").isEqualTo("Hampshire Police");

        Elements pages = document.getElementsByClass("pageSeparatedCase");
        assertThat(pages)
            .as("Incorrect number of pages")
            .hasSize(4);

        List<String> expectedOffender = List.of("Thomas Minister", "Nigel Sausage", "Joe Bloggs", "Hello World");
        AtomicInteger count = new AtomicInteger();
        pages.forEach(p -> assertThat(p.text())
            .as("Incorrect offender at index " + count.get())
            .contains(expectedOffender.get(count.getAndIncrement()))
        );
    }

    @Test
    void testSuccessfulExcelConversion() throws IOException {
        byte[] result = sjpPressListConverter.convertToExcel(getInput("/mocks/sjpPressMockJul22.json"));

        ByteArrayInputStream file = new ByteArrayInputStream(result);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        Row headingRow = sheet.getRow(0);

        assertEquals("SJP Press List", sheet.getSheetName(), "Sheet name does not match");
        assertEquals("Address", headingRow.getCell(0).getStringCellValue(),
                     "Address column is different");
        assertEquals("Case URN", headingRow.getCell(1).getStringCellValue(),
                     "Case URN column is different");
        assertEquals("Date of Birth", headingRow.getCell(2).getStringCellValue(),
                     "Date of Birth column is different");
        assertEquals("Defendant Name", headingRow.getCell(3).getStringCellValue(),
                     "Defendant Name column is different");

        // Dynamic column headings
        assertEquals("Offence 1 Press Restriction Requested", headingRow.getCell(4).getStringCellValue(),
                     "Offence 1 Press Restriction Requested column is different");
        assertEquals("Offence 1 Title", headingRow.getCell(5).getStringCellValue(),
                     "Offence 1 Title column is different");
        assertEquals("Offence 1 Wording", headingRow.getCell(6).getStringCellValue(),
                     "Offence 1 Wording column is different");

        assertEquals("Offence 2 Press Restriction Requested", headingRow.getCell(7).getStringCellValue(),
                     "Offence 2 Press Restriction Requested column is different");
        assertEquals("Offence 2 Title", headingRow.getCell(8).getStringCellValue(),
                     "Offence 2 Title column is different");
        assertEquals("Offence 2 Wording", headingRow.getCell(9).getStringCellValue(),
                     "Offence 2 Wording column is different");

        assertEquals("Prosecutor Name", headingRow.getCell(10).getStringCellValue(),
                     "Prosecutor Name column is different");
    }
}
