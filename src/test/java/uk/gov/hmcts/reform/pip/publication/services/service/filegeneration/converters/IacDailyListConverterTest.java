package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

class IacDailyListConverterTest {

    private static final IacDailyListConverter CONVERTER = new IacDailyListConverter();

    private static Document doc;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Map<String, String> metaData = Map.of("contentDate", "02 October 2022",
                                              "language", "ENGLISH",
                                              "provenance", "MANUAL_UPLOAD",
                                              "locationName", "Location Name");
        Map<String, Object> language = handleLanguage();
        JsonNode input = getInput("/mocks/iacDailyList.json");

        String result = CONVERTER.convert(input, metaData, language);
        doc = Jsoup.parse(result);
    }

    @Test
    void testSuccessfulConversionMetadata() {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(doc.getElementsByTag("h1").get(0))
            .as("Incorrect h1 element")
            .extracting(Element::text)
            .isEqualTo("Court and Tribunal Hearings Service");

        softly.assertThat(doc.getElementsByTag("h2"))
            .as("Incorrect h2 element")
            .hasSize(2)
            .extracting(Element::text)
            .containsExactly(
                "First-tier Tribunal: Immigration and Asylum Chamber",
                "Location Name Daily List"
            );

        softly.assertThat(doc.getElementsByClass("header").get(0).getElementsByTag("p"))
            .as("Incorrect p elements")
            .isNotEmpty()
            .extracting(Element::text)
            .contains(
                "List for 02 October 2022",
                "Last Updated 20 October 2022 at 9pm"
            );

        softly.assertThat(doc.getElementsByTag("section").get(0).getElementsByTag("p").get(0))
            .as("Incorrect data source")
            .extracting(Element::text)
            .isEqualTo("Data Source: MANUAL_UPLOAD");

        softly.assertAll();
    }

    @Test
    void testSuccessfulConversionBailList() {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(doc.getElementsByTag("h1").get(1))
            .as("Incorrect h1 element")
            .extracting(Element::text)
            .isEqualTo("Bail List");

        softly.assertThat(doc.getElementsByClass("govuk-accordion").get(0).getElementsByTag("p").get(0))
            .as("Incorrect room name element")
            .extracting(Element::text)
            .isEqualTo("Hearing Room: Court Room A");

        softly.assertThat(doc.getElementsByClass("govuk-table__head").get(0).getElementsByTag("th"))
            .as("Incorrect table headers")
            .hasSize(7)
            .extracting(Element::text)
            .containsExactly("Start Time",
                             "Case Ref",
                             "Appellant",
                             "Respondent",
                             "Interpreter Language",
                             "Hearing Channel",
                             "Linked Cases");

        softly.assertThat(doc.getElementsByClass("govuk-table__body").get(0).getElementsByTag("td"))
            .as("Incorrect table rows")
            .hasSize(7)
            .extracting(Element::text)
            .contains("9pm",
                      "12341234 [2 of 3]",
                      "Surname Rep: Mr Individual Forenames Individual Middlename Individual Surname",
                      "Authority Surname",
                      "French",
                      "Teams, Attended",
                      "1234");

        softly.assertAll();
    }

    @Test
    void testSuccessfulConversionNonBailList() {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(doc.getElementsByTag("h1").get(2))
            .as("Incorrect h1 element")
            .extracting(Element::text)
            .isEqualTo("Non Bail List");

        softly.assertThat(doc.getElementsByClass("govuk-accordion").get(1).getElementsByTag("p").get(0))
            .as("Incorrect room name element")
            .extracting(Element::text)
            .isEqualTo("Court Room B, Before Judge Test Name, Magistrate Test Name");

        softly.assertThat(doc.getElementsByClass("govuk-table__head").get(1).getElementsByTag("th"))
            .as("Incorrect table headers")
            .hasSize(7)
            .extracting(Element::text)
            .containsExactly("Start Time",
                             "Case Ref",
                             "Appellant",
                             "Respondent",
                             "Interpreter Language",
                             "Hearing Channel",
                             "Linked Cases");

        softly.assertThat(doc.getElementsByClass("govuk-table__body").get(1).getElementsByTag("td"))
            .as("Incorrect table rows")
            .hasSize(7)
            .extracting(Element::text)
            .contains("9:20pm",
                      "12341234 [2 of 3]",
                      "Surname Rep: Mr Individual Forenames Individual Middlename Individual Surname",
                      "Authority Surname",
                      "",
                      "Teams, Attended",
                      "");

        softly.assertAll();
    }

    private static JsonNode getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = IacDailyListConverterTest.class.getResourceAsStream(resourcePath)) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            return new ObjectMapper().readTree(inputRaw);
        }
    }

    private static Map<String, Object> handleLanguage() throws IOException {
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/en/iacDailyList.json")) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }

}
