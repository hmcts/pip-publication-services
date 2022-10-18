package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class IacDailyListConverterTest {

    private final IacDailyListConverter converter = new IacDailyListConverter();

    @Test
    void testSuccessfulConversion() throws IOException {

        Map<String, String> metaData = Map.of("contentDate", "02 October 2022",
                                              "language", "ENGLISH",
                                              "provenance", "MANUAL_UPLOAD",
                                              "locationName", "Location Name");
        Map<String, Object> language = handleLanguage();
        JsonNode input = getInput("/mocks/iacDailyList.json");

        String result = converter.convert(input, metaData, language);
        Document doc = Jsoup.parse(result);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(doc.getElementsByTag("h1"))
            .as("Incorrect h1 element")
            .hasSize(3)
            .extracting(Element::text)
            .containsExactly(
                "Court and Tribunal Hearings Service",
                "Bail List",
                "Non Bail List"
            );

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

        softly.assertThat(doc.getElementsByClass("govuk-accordion").get(0).getElementsByTag("p").get(0))
            .as("Incorrect room name element")
            .extracting(Element::text)
            .isEqualTo("Hearing Room: Court Room A");

        softly.assertThat(doc.getElementsByClass("govuk-accordion").get(1).getElementsByTag("p").get(0))
            .as("Incorrect room name element")
            .extracting(Element::text)
            .isEqualTo("Court Room B, Before Judge Test Name, Magistrate Test Name");

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

        softly.assertAll();

    }

    private JsonNode getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            return new ObjectMapper().readTree(inputRaw);
        }
    }

    private Map<String, Object> handleLanguage() throws IOException {
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/en/iacDailyList.json")) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }

}
