package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CivilAndFamilyCauseListConverterTest {
    @Autowired
    CivilAndFamilyDailyCauseListConverter civilAndFamilyDailyCauseListConverter;

    @Test
    void testFamilyCauseListTemplate() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/cy/civilAndFamilyDailyCauseList.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/",
                                                    "civilAndFamilyDailyCauseList.json")), writer,
                     Charset.defaultCharset()
        );
        Map<String, String> metadataMap = Map.of("contentDate", Instant.now().toString(),
                                                 "provenance", "provenance",
                                                 "locationName", "location"
        );

        JsonNode inputJson = new ObjectMapper().readTree(writer.toString());
        String outputHtml = civilAndFamilyDailyCauseListConverter.convert(inputJson, metadataMap, language);
        Document document = Jsoup.parse(outputHtml);
        assertThat(outputHtml).as("No html found").isNotEmpty();

        assertThat(document.title()).as("incorrect title found.")
            .isEqualTo("Civil and Family Daily Cause List");

        assertThat(document.getElementsByClass("govuk-heading-l")
                       .get(0).text())
            .as("incorrect header text").isEqualTo("Civil and Family Daily Cause List:");

        assertThat(document.getElementsByClass("govuk-body")
                       .get(2).text())
            .as("incorrect header text").contains("Last Updated 21 July 2022");
    }
}
