package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class CivilDailyCauseListConverterTest {
    private static final String OXFORD_COURT = "Oxford Combined Court Centre";
    private static final String MANUAL_UPLOAD = "MANUAL_UPLOAD";

    private static final Map<String, String> METADATA = Map.of(
        "contentDate", "1 July 2022",
        "locationName", OXFORD_COURT,
        "provenance", MANUAL_UPLOAD,
        "language", "ENGLISH"
    );
    private static final int NUMBER_OF_TABLES = 3;

    CivilDailyCauseListConverter converter = new CivilDailyCauseListConverter();

    @Test
    void testSuccessfulConversion() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/en/civilDailyCauseList.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
        String result = converter.convert(getInput("/mocks/civilDailyCauseList.json"), METADATA, language);
        Document document = Jsoup.parse(result);

        assertThat(result)
            .as("No html found")
            .isNotEmpty();

        assertThat(document.title())
            .as("incorrect document title")
            .isEqualTo("Civil Daily Cause List");

        assertFirstPageContent(document.getElementsByClass("first-page").get(0));
        assertCourtHouseInfo(document.getElementsByClass("site-address"));
        assertHearingTables(document);
        assertDataSource(document);
    }


    @Test
    void testSuccessfulConversionWelsh() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/cy/civilDailyCauseList.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
        String result = converter.convert(getInput("/mocks/civilDailyCauseList.json"), METADATA, language);
        Document document = Jsoup.parse(result);

        assertThat(result)
            .as("No html found")
            .isNotEmpty();

        assertThat(document.title())
            .as("incorrect document title")
            .isEqualTo("Rhestr Ddyddiol o Achosion Sifil");

    }

    private void assertFirstPageContent(Element element) {
        assertThat(element.getElementsByTag("h1"))
            .as("Incorrect first page h1 element")
            .hasSize(1)
            .extracting(Element::text)
            .containsExactly("Court and Tribunal Hearings Service");

        assertThat(element.getElementsByTag("h2"))
            .as("Incorrect first page h2 elements")
            .hasSize(2)
            .extracting(Element::text)
            .containsExactly("Civil Daily Cause List:",
                             "In the " + OXFORD_COURT
            );

        assertThat(element.getElementsByTag("p"))
            .as("Incorrect first page p elements")
            .hasSize(7)
            .extracting(Element::text)
            .contains("THE LAW COURTS PR1 2LL",
                      "List for 1 July 2022",
                      "Last Updated 20 April 2022 at 3:36pm"
            );
    }

    public void assertCourtHouseInfo(Elements elements) {
        assertThat(elements)
            .as("Incorrect court house info")
            .hasSize(3)
            .extracting(Element::text)
            .containsExactly(
                "Oxford Combined Court Centre, Oxford Combined",
                "Address Line 1",
                "PR1 2LL"
            );
    }

    public void assertHearingTables(Document document) {
        assertThat(document.getElementsByClass("govuk-accordion__section-heading"))
            .as("Incorrect table titles")
            .hasSize(NUMBER_OF_TABLES)
            .extracting(Element::text)
            .containsExactly(
                "Courtroom 1: Doctor, SSCS",
                "Courtroom 4",
                "Courtroom 5");

        Elements tableElements = document.getElementsByClass("govuk-table");
        assertThat(tableElements)
            .as("Incorrect number of tables")
            .hasSize(NUMBER_OF_TABLES);

        Element firstTableElement = tableElements.get(0);
        Element secondTableElement = tableElements.get(1);
        Element thirdTableElement = tableElements.get(2);

        // Assert the table columns are expected
        assertThat(getTableHeaders(firstTableElement))
            .as("Incorrect table headers")
            .hasSize(6)
            .extracting(Element::text)
            .containsExactly(
                "Time",
                "Case ID",
                "Name of party or parties involved",
                "Hearing type",
                "Location",
                "Duration"
            );

        // Assert number of rows for each table
        assertThat(getTableBodyRows(firstTableElement))
            .as("Incorrect table rows for the first table")
            .hasSize(5);
        assertThat(getTableBodyRows(secondTableElement))
            .as("Incorrect table rows for the second table")
            .hasSize(5);
        assertThat(getTableBodyRows(thirdTableElement))
            .as("Incorrect table rows for the third table")
            .hasSize(2);
    }

    private void assertDataSource(Document document) {
        Elements elements = document.getElementsByTag("p");
        assertThat(elements.get(9))
            .as("Incorrect data source")
            .extracting(Element::text)
            .isEqualTo("Data Source: " + MANUAL_UPLOAD);
    }

    private JsonNode getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            return new ObjectMapper().readTree(inputRaw);
        }
    }

    private Elements getTableHeaders(Element table) {
        return table
            .getElementsByClass("govuk-table__head")
            .get(0)
            .getElementsByClass("govuk-table__row")
            .get(0)
            .getElementsByTag("th");
    }

    private Elements getTableBodyRows(Element table) {
        return table
            .getElementsByClass("govuk-table__body")
            .get(0)
            .getElementsByClass("govuk-table__row");
    }
}
