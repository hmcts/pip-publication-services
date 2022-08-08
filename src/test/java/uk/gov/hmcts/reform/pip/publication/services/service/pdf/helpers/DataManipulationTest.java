package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private static final String FORMATTED_COURT_HOUSE_ADDRESS = "formattedCourtHouseAddress";

    private static final String COURT_ADDRESS_ERROR = "Unable to get court address address";

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
    void testFormatVenueAddressMethod() {
        List<String> venueAddress = LocationHelper.formatVenueAddress(inputJson);

        assertEquals(venueAddress.get(0), "Address Line 1",
                     "Unable to get address for venue");

        assertEquals(venueAddress.get(venueAddress.size() - 1),
                     "AA1 AA1",
                     "Unable to get address for venue");
    }

    @Test
    void testFormatCourtAddressMethod() {
        LocationHelper.formatCourtAddress(inputJson);

        assertThat(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                       .has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                       .get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .contains("Address Line 1");

        assertThat(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                       .get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as("Unable to get court address postcode")
            .contains("AA1 AA1");
    }

    @Test
    void testFormatWithNoCourtAddressMethod() {
        LocationHelper.formatCourtAddress(inputJson);

        assertThat(inputJson.get(COURT_LISTS).get(1).get(COURT_HOUSE)
                       .has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(inputJson.get(COURT_LISTS).get(1).get(COURT_HOUSE)
                       .get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEmpty();
    }

    @Test
    void testFormatCourtRoomName() {
        DataManipulation.manipulatedDailyListData(inputJson);

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
        DataManipulation.manipulatedDailyListData(inputJson);

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
        DataManipulation.manipulatedDailyListData(inputJson);

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
        DataManipulation.manipulatedDailyListData(inputJson);

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
        DataManipulation.manipulateCopListData(inputJsonCop);

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
        DataManipulation.manipulateCopListData(inputJsonCop);

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

        DataManipulation.manipulateCopListData(newInputJson);

        assertThat(newInputJson.get("regionName").asText())
            .as("Unable to get region name")
            .isEqualTo("");
    }

    @Test
    void testFormatRegionalJoh() {
        DataManipulation.manipulateCopListData(inputJsonCop);

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

        DataManipulation.manipulateCopListData(newInputJson);

        assertThat(newInputJson.get("regionalJoh").asText())
            .as("Unable to get regional Joh")
            .isEqualTo("");
    }

    @Test
    void testFormatCaseName() {
        DataManipulation.manipulatedDailyListData(inputJson);

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
        DataManipulation.manipulatedDailyListData(inputJson);

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
}
