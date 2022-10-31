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

class EtDailyListConverterTest {
    private static final String CONTENT_DATE = "20 October 2022";
    private static final String ENGLISH = "ENGLISH";
    private static final String REGION_NAME = "Test region";

    private final EtDailyListConverter converter = new EtDailyListConverter();

    @Test
    @SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
    void testSuccessfulConversion() throws IOException {
        Map<String, String> metaData = Map.of("contentDate", CONTENT_DATE,
                                              "language", ENGLISH,
                                              "region", REGION_NAME
        );
        Map<String, Object> language = handleLanguage();
        JsonNode input = getInput("/mocks/etDailyList.json");

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
            .hasSize(4)
            .extracting(Element::text)
            .containsExactly(
                "Employment Tribunals: " + REGION_NAME,
                "Daily List",
                "Venue: Leicester Crown Court",
                "Venue: Nottingham Justice Centre"
            );

        softly.assertThat(doc.getElementsByTag("h3"))
            .as("Incorrect h3 element")
            .hasSize(3)
            .extracting(Element::text)
            .containsExactly(
                "Court 1",
                "Dynatrace Suite 2",
                "Jenkins Room 3"
            );

        softly.assertThat(doc.getElementById("content-date"))
            .as("Incorrect content date")
            .extracting(Element::text)
            .isEqualTo("List for " + CONTENT_DATE);

        softly.assertThat(doc.getElementById("publication-date"))
            .as("Incorrect publication date")
            .extracting(Element::text)
            .isEqualTo("Last Updated 13 February 2022 at 9:30am");

        softly.assertThat(doc.getElementsByTag("th"))
            .as("Incorrect table headers")
            .hasSize(24)
            .extracting(Element::text)
            .startsWith("Start Time",
                        "Duration",
                        "Case Number",
                        "Claimant",
                        "Respondent",
                        "Hearing Type",
                        "Jurisdiction",
                        "Hearing Platform"
            );

        softly.assertThat(doc.getElementsByTag("td"))
            .as("Incorrect table contents")
            .hasSize(40)
            .extracting(Element::text)
            .startsWith("9:30am",
                        "2 hours [2 of 3]",
                        "12341234",
                        "HRH G Anderson Rep: Mr R Hargreaves",
                        "Capt. S Jenkins Rep: Dr M Naylor",
                        "This is a hearing type",
                        "This is a case type",
                        "This is a sitting channel"
            );

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
            .getContextClassLoader().getResourceAsStream("templates/languages/en/etDailyList.json")) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }
}
