package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
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
@SuppressWarnings("PMD.TooManyMethods")
class DataManipulationTest {
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String CASE = "case";
    private static final String CASE_NAME = "caseName";
    private static final String CASE_TYPE = "caseType";

    private static JsonNode inputJson;
    private static JsonNode inputJsonCop;

    @BeforeAll
    public static void setup()  throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/familyDailyCauseList.json")), writer,
                     Charset.defaultCharset()
        );

        inputJson = new ObjectMapper().readTree(writer.toString());

        StringWriter copWriter = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/copDailyCauseList.json")), copWriter,
                     Charset.defaultCharset()
        );

        inputJsonCop = new ObjectMapper().readTree(copWriter.toString());
    }

    @Test
    void testFormatCourtRoomName() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get("formattedSessionCourtRoom").asText())
            .as("Unable to get courtroom name")
            .isEqualTo("This is the court room name");
    }

    @Test
    void testFormatHearingDuration() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get("formattedDuration").asText())
            .as("Unable to get duration")
            .isEqualTo("1 hour 25 mins");
    }

    @Test
    void testFormatHearingTime() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get("time").asText())
            .as("Unable to get hearing time")
            .isEqualTo("10:30");
    }

    @Test
    void testFormatHearingChannel() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get("caseHearingChannel").asText())
            .as("Unable to get case hearing channel")
            .isEqualTo("Teams, Attended");
    }

    @Test
    void testFormatCaseIndicator() {
        DataManipulation.manipulateCopListData(inputJsonCop, Language.ENGLISH);

        assertThat(inputJsonCop.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get(HEARING).get(0)
                       .get(CASE).get(0)
                       .get("caseIndicator").asText())
            .as("Unable to get case name")
            .contains("[1 of 2]");
    }

    @Test
    void testFindAndManipulateJudiciary() {
        DataManipulation.manipulateCopListData(inputJsonCop, Language.ENGLISH);

        assertThat(inputJsonCop.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get("formattedSessionJoh").asText())
            .as("Unable to get session Joh")
            .contains("Mrs Firstname Surname");

        assertThat(inputJsonCop.get(COURT_LISTS).get(1)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get("formattedSessionJoh").asText())
            .as("Unable to get session Joh")
            .contains("Mrs Firstname Surname, Mrs OtherFirstname OtherSurname");
    }

    @Test
    void testFormatRegionNameWhenNotPresent() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(
            Paths.get("src/test/resources/mocks/copDailyCauseListMissingRegion.json")), writer,
                     Charset.defaultCharset()
        );

        JsonNode newInputJson = new ObjectMapper().readTree(writer.toString());

        DataManipulation.manipulateCopListData(newInputJson, Language.ENGLISH);

        assertThat(newInputJson.get("regionName").asText())
            .as("Unable to get region name")
            .isEqualTo("");
    }

    @Test
    void testFormatRegionalJoh() {
        DataManipulation.manipulateCopListData(inputJsonCop, Language.ENGLISH);

        assertThat(inputJsonCop.get("regionalJoh").asText())
            .as("Unable to get regional Joh")
            .contains("Judge Firstname Surname");
    }

    @Test
    void testMissingRegionalJoh() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(
            Paths.get("src/test/resources/mocks/copDailyCauseListMissingRegionalJoh.json")), writer,
                     Charset.defaultCharset()
        );

        JsonNode newInputJson = new ObjectMapper().readTree(writer.toString());

        DataManipulation.manipulateCopListData(newInputJson, Language.ENGLISH);

        assertThat(newInputJson.get("regionalJoh").asText())
            .as("Unable to get regional Joh")
            .isEqualTo("");
    }

    @Test
    void testFormatCaseName() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get(HEARING).get(0)
                       .get(CASE).get(0)
                       .get(CASE_NAME).asText())
            .as("Unable to get case name")
            .contains("[2 of 3]");
    }

    @Test
    void testCaseType() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH, false);

        assertThat(inputJson.get(COURT_LISTS).get(0)
                       .get(COURT_HOUSE)
                       .get(COURT_ROOM).get(0)
                       .get(SESSION).get(0)
                       .get(SITTINGS).get(0)
                       .get(HEARING).get(0)
                       .get(CASE).get(0)
                       .get(CASE_TYPE).asText())
            .as("Unable to get case type")
            .isEqualTo("normal");
    }

    @Test
    void testFindManipulatePartyInformationApplicant() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .startsWith("Applicant Title Applicant Forename Applicant Middlename Applicant Surname");
    }

    @Test
    void testFindManipulatePartyInformationApplicantRepresentativeEnglish() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .contains("Legal Advisor: Rep Title Rep Forename Rep Middlename Rep Surname");
    }

    @Test
    void testFindManipulatePartyInformationApplicantRepresentativeWelsh() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.WELSH);

        assertThat(inputJson.get("applicant").asText())
            .as("applicant is incorrect")
            .contains("Cynghorydd Cyfreithiol: Rep Title Rep Forename Rep Middlename Rep Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondent() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("respondent").asText())
            .as("respondent is incorrect")
            .startsWith("Title Forename Middlename Surname");
    }

    @Test
    void testFindManipulatePartyInformationProsecutingAuthority() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("prosecutingAuthority").asText())
            .as("prosecuting authority is incorrect")
            .isEqualTo("Title Forename Middlename Surname");
    }

    @Test
    void testFindManipulatePartyInformationRespondentRepresentative() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("respondent").asText())
            .as("respondent is incorrect")
            .endsWith("Legal Advisor: Mr ForenameB MiddlenameB SurnameB");
    }

    @Test
    void testFindManipulatePartyInformationClaimant() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("claimant").asText())
            .as("claimant is incorrect")
            .isEqualTo("Claimant Title Claimant Forename Claimant Middlename Claimant Surname");
    }

    @Test
    void testFindManipulatePartyInformationClaimantRepresentative() throws IOException {
        JsonNode inputJson = loadInPartyFile();

        DataManipulation.findAndManipulatePartyInformation(inputJson, Language.ENGLISH);

        assertThat(inputJson.get("claimantRepresentative").asText())
            .as("claimant representative is incorrect")
            .isEqualTo("Rep Title Rep Forename Rep Middlename Rep Surname");
    }

    private JsonNode loadInPartyFile() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(
                         Paths.get("src/test/resources/mocks/partyManipulationJson.json")), writer,
                     Charset.defaultCharset()
        );

        return new ObjectMapper().readTree(writer.toString());
    }

}
