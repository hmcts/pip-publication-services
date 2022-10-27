package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

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
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DailyCauseListHelper.preprocessArtefactForThymeLeafConverter;

@ActiveProfiles("test")
class EtFortnightlyPressListHelperTest {
    private static JsonNode inputJson;
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String CASE = "case";
    public static final String PROVENANCE = "provenance";
    Map<String, Object> language =
            Map.of("rep", "Rep: ",
                   "noRep", "No Representative");

    Map<String, String> metadataMap = Map.of("contentDate", Instant.now().toString(),
                                             PROVENANCE, PROVENANCE,
                                             "locationName", "location",
                                             "language", "ENGLISH"
    );

    @BeforeAll
    public static void setup()  throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths
            .get("src/test/resources/mocks/etFortnightlyPressList.json")), writer,
                     Charset.defaultCharset()
        );

        inputJson = new ObjectMapper().readTree(writer.toString());
    }

    @Test
    void testEtFortnightlyListFormattedMethod() {
        preprocessArtefactForThymeLeafConverter(inputJson, metadataMap, language, true);
        EtFortnightlyPressListHelper.etFortnightlyListFormatted(inputJson, language);

        assertEquals(inputJson.get(COURT_LISTS).size(), 2,
                     "Unable to find correct court List array");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                        .get(COURT_ROOM).get(0).get(SESSION).get(0)
                        .get(SITTINGS).get(0).get("sittingDate").asText(),
                        "Monday 14 February 2022",
                    "Unable to find sitting date");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("courtRoom").asText(),
                     "Court 1",
                     "Unable to find court room");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("time").asText(),
                     "9:30am",
                     "Unable to find time");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("formattedDuration").asText(),
                     "2 hours",
                     "Unable to find duration");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(0)
                         .get("caseNumber").asText(),
                     "12341234",
                     "Unable to find case number");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("claimant").asText(),
                     "HRH G Anderson",
                     "Unable to find claimant");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("claimantRepresentative").asText(),
                     "Rep: Mr R Hargreaves",
                     "Unable to find claimant representative");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("respondent").asText(),
                     "Capt. S Jenkins",
                     "Unable to find respondent");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("respondentRepresentative").asText(),
                     "Rep: Dr M Naylor",
                     "Unable to find respondent representative");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("hearingType").asText(),
                     "This is a hearing type",
                     "Unable to find hearing type");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get(CASE).get(0)
                         .get("caseType").asText(),
                     "This is a case type",
                     "Unable to find Jurisdiction");
        assertEquals(inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE)
                         .get(COURT_ROOM).get(0).get(SESSION).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(0)
                         .get("caseHearingChannel").asText(),
                     "This is a sitting channel",
                     "Unable to find Hearing Platform");
    }

    @Test
    void testSplitByCourtAndDateMethod() {
        preprocessArtefactForThymeLeafConverter(inputJson, metadataMap, language, true);
        EtFortnightlyPressListHelper.etFortnightlyListFormatted(inputJson,language);
        EtFortnightlyPressListHelper.splitByCourtAndDate(inputJson);

        assertEquals(inputJson.get(COURT_LISTS).size(), 2,
                     "Unable to find correct court List array");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get("sittingDate").asText(),
                     "Monday 14 February 2022",
                     "Unable to find sitting date");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("courtRoom").asText(),
                     "Court 1",
                     "Unable to find court room");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("time").asText(),
                     "9:30am",
                     "Unable to find time");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("formattedDuration").asText(),
                     "2 hours",
                     "Unable to find duration");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get(CASE).get(0)
                         .get("caseNumber").asText(),
                     "12341234",
                     "Unable to find case number");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("claimant").asText(),
                     "HRH G Anderson",
                     "Unable to find claimant");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("claimantRepresentative").asText(),
                         "Rep: Mr R Hargreaves",
                         "Unable to find claimant representative");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("respondent").asText(),
                     "Capt. S Jenkins",
                     "Unable to find respondent");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("respondentRepresentative").asText(),
                     "Rep: Dr M Naylor",
                     "Unable to find respondent representative");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("hearingType").asText(),
                     "This is a hearing type",
                     "Unable to find hearing type");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get(CASE).get(0)
                         .get("caseType").asText(),
                     "This is a case type",
                     "Unable to find Jurisdiction");
        assertEquals(inputJson.get(COURT_LISTS).get(0)
                         .get(SITTINGS).get(0).get(HEARING).get(1).get(0)
                         .get("caseHearingChannel").asText(),
                     "This is a sitting channel",
                     "Unable to find Hearing Platform");
    }
}
