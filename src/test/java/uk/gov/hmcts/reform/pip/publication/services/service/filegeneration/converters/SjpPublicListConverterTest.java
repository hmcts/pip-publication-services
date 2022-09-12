package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SjpPublicListConverterTest {
    private final SjpPublicListConverter converter = new SjpPublicListConverter();
    private final Map<String, String> metaData = Collections.singletonMap("contentDate", "1 July 2022");

    @Test
    void testSuccessfulConversion() throws IOException {
        String result = converter.convert(getInput("/mocks/sjpPublicList.json"), metaData);
        Document doc = Jsoup.parse(result);
        assertTitleAndDescription(doc);

        assertThat(doc.getElementsByTag("td"))
            .as("Incorrect table contents")
            .hasSize(8)
            .extracting(Element::text)
            .containsExactly("This is a forename This is a surname",
                             "AA1 AA1",
                             "This is an offence title",
                             "This is an organisation",
                             "This is a forename2 This is a surname2",
                             "AA2 AA2",
                             "This is an offence title2",
                             "This is an organisation2");
    }

    @Test
    void testConversionWithMissingField() throws IOException {
        String result = converter.convert(getInput("/mocks/sjpPublicListMissingPostcode.json"), metaData);
        Document doc = Jsoup.parse(result);
        assertTitleAndDescription(doc);

        // Assert that the record with missing postcode is not shown in the HTML
        assertThat(doc.getElementsByTag("td"))
            .as("Incorrect table contents")
            .hasSize(4)
            .extracting(Element::text)
            .containsExactly("This is a forename2 This is a surname2",
                             "AA2 AA2",
                             "This is an offence title2",
                             "This is an organisation2");
    }

    private JsonNode getInput(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            return new ObjectMapper().readTree(inputRaw);
        }
    }

    @SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
    private void assertTitleAndDescription(Document doc) {
        assertThat(doc.getElementsByTag("h1"))
            .as("Incorrect h1 element")
            .hasSize(1)
            .extracting(Element::text)
            .contains("Court and Tribunal Hearings Service");

        assertThat(doc.getElementsByTag("h2"))
            .as("Incorrect h2 element")
            .hasSize(1)
            .extracting(Element::text)
            .contains("Single Justice Procedure Public List");

        assertThat(doc.getElementsByTag("h3"))
            .as("Incorrect h3 element")
            .hasSize(1)
            .extracting(Element::text)
            .contains("Single Justice Procedure cases that are ready for hearing");

        assertThat(doc.getElementsByClass("header").get(0).getElementsByTag("p"))
            .as("Incorrect p elements")
            .hasSize(2)
            .extracting(Element::text)
            .containsExactly("List for 1 July 2022",
                             "Published: 14 September 2016 at 00:30");

        assertThat(doc.getElementsByTag("th"))
            .as("Incorrect table headers")
            .hasSize(4)
            .extracting(Element::text)
            .containsExactly("Name", "Postcode", "Offence", "Prosecutor");
    }
}
