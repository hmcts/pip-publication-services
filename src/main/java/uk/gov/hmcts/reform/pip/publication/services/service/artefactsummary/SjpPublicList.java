package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class SjpPublicList {

    private static final String HEARING = "hearing";
    private static final String OFFENCE = "offence";
    private static final String INDIVIDUAL_DETAILS = "individualDetails";
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";


    /**
     * parent method for sjp public lists. iterates on sittings.
     *
     * @param payload - json body.
     * @return string of data.
     * @throws JsonProcessingException - jackson prereq.
     */
    public String artefactSummarysjpPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> sittings =
            node.get(COURT_LISTS).get(0).get(COURT_HOUSE).get(COURT_ROOM).get(0)
                .get(SESSION).get(0).get(SITTINGS).elements();
        while (sittings.hasNext()) {
            output.append('•');
            JsonNode currentHearing = sittings.next();
            output.append(processRolesSjpPublic(currentHearing));
            String offence = currentHearing.get(HEARING).get(0).get(OFFENCE).get(0).get("offenceTitle").asText();
            output.append("Offence: ").append(offence).append('\n');
        }
        return output.toString();
    }

    /**
     * handle sjp public roles iteration.
     * @param hearing - node of a given hearing.
     * @return string of roles.
     */
    private String processRolesSjpPublic(JsonNode hearing) {
        StringBuilder outputString = new StringBuilder();
        Iterator<JsonNode> partyNames = hearing.get(HEARING).get(0).get("party").elements();
        while (partyNames.hasNext()) {
            JsonNode currentParty = partyNames.next();
            switch (currentParty.get("partyRole").asText()) {
                case "ACCUSED":
                    String forenames = currentParty.get(INDIVIDUAL_DETAILS).get("individualForenames").asText();
                    String surname = currentParty.get(INDIVIDUAL_DETAILS).get("individualSurname").asText();
                    String postCode = currentParty.get(INDIVIDUAL_DETAILS).get("address").get("postCode").asText();
                    outputString.append("Defendant: ").append(forenames).append(' ').append(surname);
                    outputString.append("\nPostcode: ").append(postCode).append('\n');
                    break;
                case "PROSECUTOR":
                    outputString.append("Prosecutor: ")
                        .append(currentParty.get("organisationDetails").get("organisationName").asText())
                        .append('\n');
                    break;
                default:
                    break;
            }
        }
        return outputString.toString();
    }
}
