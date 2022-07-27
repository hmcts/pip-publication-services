package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FamilyCauseListConverterTest {
    @Autowired
    FamilyDailyCauseListConverter familyDailyCauseListConverter;

    @Test
    void testSjpPressListTemplate() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/",
            "familyDailyCauseList.json")), writer,
                     Charset.defaultCharset()
        );
        Map<String, String> metadataMap = Map.of("contentDate", Instant.now().toString(),
                                                 "provenance", "provenance",
                                                 "locationName", "location"
        );

        JsonNode inputJson = new ObjectMapper().readTree(writer.toString());
        String outputHtml = familyDailyCauseListConverter.convert(inputJson, metadataMap);
        Document document = Jsoup.parse(outputHtml);
        assertThat(outputHtml).as("No html found").isNotEmpty();

        assertThat(document.title()).as("incorrect title found.")
            .isEqualTo("Family Daily Cause List");

        assertThat(document.getElementsByClass("govuk-heading-l")
            .get(0).text())
            .as("incorrect header text").isEqualTo("Family Daily Cause List:");

        assertThat(document.getElementsByClass("govuk-body")
                       .get(2).text())
            .as("incorrect header text").isEqualTo("Last Updated 21 July 2022 at 03:01pm");
    }
}
