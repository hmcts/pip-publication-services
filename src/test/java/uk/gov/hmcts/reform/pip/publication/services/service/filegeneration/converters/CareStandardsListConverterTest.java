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

class CareStandardsListConverterTest {
    private final CareStandardsListConverter converter = new CareStandardsListConverter();

    @Test
    @SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
    void testSuccessfulConversion() throws IOException {
        Map<String, String> metaData = Map.of("contentDate", "02 October 2022",
                                              "language", "ENGLISH");
        Map<String, Object> language = handleLanguage();
        JsonNode input = getInput("/mocks/careStandardsList.json");

        String result = converter.convert(input, metaData, language);
        Document doc = Jsoup.parse(result);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(doc.getElementsByTag("h1"))
            .as("Incorrect h1 element")
            .hasSize(1)
            .extracting(Element::text)
            .contains("Court and Tribunal Hearings Service");

        softly.assertThat(doc.getElementsByTag("h2"))
            .as("Incorrect h2 element")
            .hasSize(2)
            .extracting(Element::text)
            .containsExactly(
                "Care Standards",
                "Tribunal Hearing List"
            );

        softly.assertThat(doc.getElementsByClass("header").get(0).getElementsByTag("p"))
            .as("Incorrect p elements")
            .isNotEmpty()
            .extracting(Element::text)
            .contains(
                "List for 02 October 2022",
                "Last Updated 04 October 2022 at 10am"
            );

        softly.assertThat(doc.getElementsByTag("th"))
            .as("Incorrect table headers")
            .hasSize(5)
            .extracting(Element::text)
            .containsExactly("Hearing Date", "Case Name", "Duration", "Hearing Type", "Venue");

        softly.assertThat(doc.getElementsByTag("td"))
            .as("Incorrect table contents")
            .hasSize(15)
            .extracting(Element::text)
            .containsExactly("05 October",
                             "A Vs B",
                             "1 day [1 of 2]",
                             "Remote - Teams",
                             "The Court House Court Street SK4 5LE",
                             "05 October",
                             "A Vs B",
                             "1 day [2 of 2]",
                             "Remote - Teams",
                             "The Court House Court Street SK4 5LE",
                             "06 October",
                             "C Vs D",
                             "2 hours 30 mins",
                             "Video",
                             "The Court House Court Street SK4 5LE");

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
            .getContextClassLoader().getResourceAsStream("templates/languages/en/careStandardsList.json")) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }
}
