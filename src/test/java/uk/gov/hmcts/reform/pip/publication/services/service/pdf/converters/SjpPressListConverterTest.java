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
import uk.gov.hmcts.reform.pip.publication.services.Application;
import uk.gov.hmcts.reform.pip.publication.services.configuration.WebClientConfigurationTest;

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

@ActiveProfiles("test")
@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class})
class SjpPressListConverterTest {

    @Autowired
    SjpPressListConverter sjpPressListConverter;

    public static final String PROVENANCE = "provenance";

    @Test
    void testSjpPressListTemplate() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/en/sjpPressList.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "sjpPressMockJul22.json")), writer,
                     Charset.defaultCharset()
        );
        Map<String, String> metadataMap = Map.of("contentDate", Instant.now().toString(),
                                                 PROVENANCE, PROVENANCE,
                                                 "locationName", "location",
                                                 "language", "ENGLISH"
        );

        JsonNode inputJson = new ObjectMapper().readTree(writer.toString());
        String outputHtml = sjpPressListConverter.convert(inputJson, metadataMap, language);
        Document document = Jsoup.parse(outputHtml);
        assertThat(outputHtml).as("No html found").isNotEmpty();

        assertThat(document.title()).as("incorrect title found.")
            .contains("Single Justice Procedure - Press List");

        assertThat(document.getElementsByClass("mainHeaderText")
                       .select(".mainHeaderText > h1:nth-child(1)").text())
            .as("incorrect header text").isEqualTo("Single Justice Procedure - Press List");

        assertThat(document.select(
            "div.pageSeparatedCase:nth-child(2) > table:nth-child(3) > tbody:nth-child(1) >"
                + " tr:nth-child(7) > td:nth-child(2)").text())
            .as("incorrect value found").isEqualTo("Hampshire Police");
    }

    @Test
    void testSjpPressListTemplateWelsh() throws IOException {
        Map<String, Object> language;
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream("templates/languages/cy/sjpPressList.json")) {
            language = new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "sjpPressMockJul22.json")), writer,
                     Charset.defaultCharset()
        );
        Map<String, String> metadataMap = Map.of("contentDate",
                                                 Instant.now().toString(),
                                                 PROVENANCE, PROVENANCE,
                                                 "locationName", "location",
                                                 "language", "WELSH"
        );

        JsonNode inputJson = new ObjectMapper().readTree(writer.toString());
        String outputHtml = sjpPressListConverter.convert(inputJson, metadataMap, language);
        Document document = Jsoup.parse(outputHtml);
        assertThat(outputHtml).as("No html found").isNotEmpty();

        assertThat(document.title()).as("incorrect title found.")
            .contains("Gweithdrefn Un Ynad - Rhestr Wasg ");

        assertThat(document.getElementsByClass("mainHeaderText")
                       .select(".mainHeaderText > h1:nth-child(1)").text())
            .as("incorrect header text").isEqualTo("Gweithdrefn Un Ynad - Rhestr Wasg");

        assertThat(document.select(
            "div.pageSeparatedCase:nth-child(2) > table:nth-child(3) > tbody:nth-child(1) >"
                + " tr:nth-child(7) > td:nth-child(2)").text())
            .as("incorrect value found").isEqualTo("Hampshire Police");
    }
}
