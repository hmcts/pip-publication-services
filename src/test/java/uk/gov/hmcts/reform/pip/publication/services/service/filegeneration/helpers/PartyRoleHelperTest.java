package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class PartyRoleHelperTest {
    private JsonNode loadInPartyFile() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(
            Paths.get("src/test/resources/mocks/partyManipulationJson.json")), writer,
                     Charset.defaultCharset()
        );

        return new ObjectMapper().readTree(writer.toString());
    }

    @Test
    void testFindManipulatePartyInformationApplicant() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .startsWith("Applicant Title Applicant Forename Applicant Middlename Applicant Surname");
    }

    @Test
    void testFindManipulatePartyInformationApplicantRepresentativeEnglish() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .contains("Legal Advisor: Rep Title Rep Forename Rep Middlename Rep Surname");
    }

    @Test
    void testFindManipulatePartyInformationApplicantRepresentativeWelsh() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.WELSH, false);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .contains("Cynghorydd Cyfreithiol: Rep Title Rep Forename Rep Middlename Rep Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondent() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("respondent").asText())
            .as("respondent is incorrect")
            .startsWith("Title Forename Middlename Surname");
    }

    @Test
    void testFindManipulatePartyInformationProsecutingAuthority() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("prosecutingAuthority").asText())
            .as("prosecuting authority is incorrect")
            .isEqualTo("Title Forename Middlename Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondentRepresentative() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("respondent").asText())
            .as("respondent is incorrect")
            .endsWith("Legal Advisor: Mr ForenameB MiddlenameB SurnameB");
    }

    @Test
    void testFindManipulatePartyInformationClaimant() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("claimant").asText())
            .as("claimant is incorrect")
            .isEqualTo("Claimant Title Claimant Forename Claimant Middlename Claimant Surname");
    }

    @Test
    void testFindManipulatePartyInformationClaimantRepresentative() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        PartyRoleHelper.findAndManipulatePartyInformation(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get("claimantRepresentative").asText())
            .as("claimant representative is incorrect")
            .isEqualTo("Rep Title Rep Forename Rep Middlename Rep Surname");
    }
}
