package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

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

@ActiveProfiles("test")
class LocationHelperTest {
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String FORMATTED_COURT_HOUSE_ADDRESS = "formattedCourtHouseAddress";
    private static final String VENUE_ADDRESS_ERROR = "Incorrect venue address";
    private static final String COURT_ADDRESS_ERROR = "Incorrect court house address";
    private static final String DELIMITER = "|";

    private static JsonNode inputJson;

    @BeforeAll
    public static void setup()  throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/familyDailyCauseList.json")),
                     writer, Charset.defaultCharset()
        );

        inputJson = new ObjectMapper().readTree(writer.toString());
    }

    @Test
    void testFormatVenueAddress() {
        List<String> venueAddress = LocationHelper.formatVenueAddress(inputJson);

        assertThat(venueAddress.get(0))
            .as(VENUE_ADDRESS_ERROR)
            .isEqualTo("Address Line 1");

        assertThat(venueAddress.get(venueAddress.size() - 1))
            .as(VENUE_ADDRESS_ERROR)
            .isEqualTo("AA1 AA1");
    }

    @Test
    void testFormatCourtAddress() {
        LocationHelper.formatCourtAddress(inputJson, DELIMITER);
        JsonNode courtHouse = inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE);

        assertThat(courtHouse.has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(courtHouse.get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEqualTo("Address Line 1|Venue Town|Venue County|AA1 AA1");
    }

    @Test
    void testFormatCourtAddressWithNewLineDelimiter() {
        LocationHelper.formatCourtAddress(inputJson, "\n");
        JsonNode courtHouse = inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE);

        assertThat(courtHouse.has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(courtHouse.get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEqualTo("Address Line 1\nVenue Town\nVenue County\nAA1 AA1");
    }

    @Test
    void testFormatCourtAddressIncludingCourtHouseName() {
        LocationHelper.formatCourtAddress(inputJson, DELIMITER, true);
        JsonNode courtHouse = inputJson.get(COURT_LISTS).get(0).get(COURT_HOUSE);

        assertThat(courtHouse.has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(courtHouse.get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEqualTo("This is the site name|Address Line 1|Venue Town|Venue County|AA1 AA1");
    }

    @Test
    void testFormatWithNoCourtAddress() {
        LocationHelper.formatCourtAddress(inputJson, DELIMITER);
        JsonNode courtHouse = inputJson.get(COURT_LISTS).get(1).get(COURT_HOUSE);

        assertThat(courtHouse.has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(courtHouse.get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEmpty();
    }

    @Test
    void testFormatWithCourtHouseNameOnly() {
        LocationHelper.formatCourtAddress(inputJson, DELIMITER, true);
        JsonNode courtHouse = inputJson.get(COURT_LISTS).get(1).get(COURT_HOUSE);

        assertThat(courtHouse.has(FORMATTED_COURT_HOUSE_ADDRESS))
            .as(COURT_ADDRESS_ERROR)
            .isTrue();

        assertThat(courtHouse.get(FORMATTED_COURT_HOUSE_ADDRESS).asText())
            .as(COURT_ADDRESS_ERROR)
            .isEqualTo("This is the site name");
    }
}
