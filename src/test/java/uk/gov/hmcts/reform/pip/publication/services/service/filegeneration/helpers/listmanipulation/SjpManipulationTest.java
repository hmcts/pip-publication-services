package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPublicList;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class SjpManipulationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static JsonNode topLevelNode;

    @BeforeAll
    public static void setup() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/sjpPublicList.json")),
                     writer,
                     Charset.defaultCharset()
        );
        topLevelNode = OBJECT_MAPPER.readTree(writer.toString());
    }

    @Test
    void testSjpCaseIsGeneratedWhenAllAttributesPresent() {
        JsonNode hearingNode = topLevelNode.get("courtLists").get(0)
            .get("courtHouse")
            .get("courtRoom").get(0)
            .get("session").get(0)
            .get("sittings").get(0)
            .get("hearing").get(0);

        SjpPublicList expectedSjpCase = new SjpPublicList(
            "This is a forename This is a surname",
            "AA1 AA1",
            "Offence A, Offence B",
            "This is an organisation"
        );

        assertThat(SjpManipulation.constructSjpCase(hearingNode))
            .isPresent()
            .hasValue(expectedSjpCase);
    }

    @Test
    void testSjpCaseIsNotGeneratedWhenAttributeMissing() {
        JsonNode hearingNode = topLevelNode.get("courtLists").get(0)
            .get("courtHouse")
            .get("courtRoom").get(0)
            .get("session").get(0)
            .get("sittings").get(2)
            .get("hearing").get(0);

        assertThat(SjpManipulation.constructSjpCase(hearingNode)).isEmpty();
    }
}

