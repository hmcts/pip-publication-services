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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class CrownDailyListHelperTest {
    private static JsonNode inputJson;
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String CASE = "case";

    @BeforeAll
    public static void setup()  throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/crownDailyList.json")), writer,
                     Charset.defaultCharset()
        );

        inputJson = new ObjectMapper().readTree(writer.toString());
    }

    @Test
    void testFindUnallocatedCasesInCrownDailyListDataMethod() {
        assertEquals(inputJson.get(COURT_LISTS).size(), 4,
                     "Unable to find correct court List array");
        CrownDailyListHelper.findUnallocatedCasesInCrownDailyListData(inputJson);
        assertEquals(inputJson.get(COURT_LISTS).size(), 5,
                     "Unable to find correct court List array when unallocated cases are there");
        assertTrue(inputJson.get(COURT_LISTS).get(4).get("unallocatedCases").asBoolean(),
                     "Unable to find unallocated case section");
        assertFalse(inputJson.get(COURT_LISTS).get(0).get("unallocatedCases").asBoolean(),
                     "Unable to find allocated case section");
        assertTrue(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(1)
                         .get("exclude").asBoolean(),
                     "Unable to find unallocated courtroom");
        assertEquals(inputJson.get(COURT_LISTS).get(4).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get("courtRoomName").asText(), "to be allocated",
                     "Unable to find unallocated courtroom");
    }

    @Test
    void testFormattedCourtRoomNameMethod() {
        DataManipulation.manipulatedDailyListData(inputJson, Language.ENGLISH);
        CrownDailyListHelper.manipulatedCrownDailyListData(inputJson);
        CrownDailyListHelper.findUnallocatedCasesInCrownDailyListData(inputJson);
        CrownDailyListHelper.formattedCourtRoomName(inputJson);

        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get("formattedSessionCourtRoom").asText(),
                     "1: Firstname1 Surname1, Firstname2 Surname2",
                     "Unable to find formatted courtroom name");

        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(1)
                         .get(SESSION).get(0).get("formattedSessionCourtRoom").asText(), "to be allocated",
                     "Unable to find unallocated formatted courtroom name");

        assertEquals(inputJson.get(COURT_LISTS).get(1).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get("formattedSessionCourtRoom").asText(), "CourtRoom 1",
                     "Unable to find formatted courtroom name without judge");
    }

    @Test
    void testManipulatedCrownDailyListDataMethod() {
        CrownDailyListHelper.manipulatedCrownDailyListData(inputJson);

        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get("time").asText(), "10:40am",
                     "Unable to find correct case time");
        assertEquals(inputJson.get(COURT_LISTS).get(2).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get("time").asText(), "1:00pm",
                     "Unable to find correct case time");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("defendant").asText(), "Defendant_SN, Defendant_FN",
                     "Unable to find information for defendant");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("prosecuting_authority").asText(), "Pro_Auth_SN, Pro_Auth_FN",
                     "Unable to find information for prosecution authority");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(0).get("linkedCases").asText(), "caseid111, caseid222",
                     "Unable to find linked cases for a particular case");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(1).get("linkedCases").asText(), "",
                     "able to find linked cases for a particular case");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("listingNotes").asText(), "Listing details text",
                     "Unable to find listing notes for a particular hearing");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(1)
                         .get("listingNotes").asText(), "",
                     "Able to find listing notes for a particular hearing");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(0).get("caseCellBorder").asText(), "no-border-bottom",
                     "Unable to find linked cases css for a particular case");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                         .get(SESSION).get(0).get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(0).get("linkedCasesBorder").asText(), "no-border-bottom",
                     "Unable to find linked cases css for a particular case");
    }
}
