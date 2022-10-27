package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.TribunalNationalList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.LocationHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TribunalNationalListsManipulationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static JsonNode topLevelNode;

    @BeforeAll
    public static void setup() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(
            Files.newInputStream(Paths.get("src/test/resources/mocks/careStandardsList.json")),
            writer,
            Charset.defaultCharset()
        );
        topLevelNode = OBJECT_MAPPER.readTree(writer.toString());
    }

    @Test
    void testTribunalNationalListsManipulation() {
        LocationHelper.formatCourtAddress(topLevelNode, ", ", true);
        List<TribunalNationalList> tribunalNationalList =
            TribunalNationalListsManipulation.processRawListData(topLevelNode, Language.ENGLISH);

        assertThat(tribunalNationalList.get(0).getHearingDate())
            .as("Unable to get hearing date")
            .isEqualTo("05 October");

        assertThat(tribunalNationalList.get(0).getCaseName())
            .as("Unable to get case name")
            .isEqualTo("A Vs B");

        assertThat(tribunalNationalList.get(0).getDuration())
            .as("Unable to get duration")
            .isEqualTo("1 day [1 of 2]");

        assertThat(tribunalNationalList.get(0).getHearingType())
            .as("Unable to get hearing type")
            .isEqualTo("Remote - Teams");

        assertThat(tribunalNationalList.get(0).getVenue())
            .as("Unable to get venue")
            .isEqualTo("The Court House, Court Street, SK4 5LE");
    }
}
