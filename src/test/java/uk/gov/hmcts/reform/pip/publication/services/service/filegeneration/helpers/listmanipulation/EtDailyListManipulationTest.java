package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EtDailyListManipulationTest {
    JsonNode inputJson;

    @BeforeAll
    void setup() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
            "/mocks/partyManipulation.json")) {
            String inputRaw = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputJson = new ObjectMapper().readTree(inputRaw);
        }
    }

    @Test
    void testFindManipulatePartyInformationClaimant() {
        EtDailyListManipulation.handleParties(inputJson);
        assertThat(inputJson.get("claimant").asText())
            .as("Incorrect claimant")
            .isEqualTo("Claimant Title C Claimant Surname");
    }

    @Test
    void testFindManipulatePartyInformationClaimantRepresentative() {
        EtDailyListManipulation.handleParties(inputJson);
        assertThat(inputJson.get("claimantRepresentative").asText())
            .as("Incorrect claimant representative")
            .isEqualTo("Rep Title R Rep Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondent() {
        EtDailyListManipulation.handleParties(inputJson);
        assertThat(inputJson.get("respondent").asText())
            .as("Incorrect respondent")
            .isEqualTo("Title F Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondentRepresentative() {
        EtDailyListManipulation.handleParties(inputJson);
        assertThat(inputJson.get("respondentRepresentative").asText())
            .as("Incorrect respondent representative")
            .isEqualTo("Mr F SurnameB");
    }
}
