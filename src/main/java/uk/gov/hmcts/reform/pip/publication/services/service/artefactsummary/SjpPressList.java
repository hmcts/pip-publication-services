package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Locale;

@Service
public class SjpPressList {


    private static final String INDIVIDUAL_DETAILS = "individualDetails";
    private static final String SESSION = "session";

    /**
     * sjp press parent method - iterates over session data. Routes to specific methods which handle offences and
     * judiciary roles.
     *
     * @param payload - json body.
     * @return String with final summary data.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummarysjpPress(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get(SESSION).get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (endNode.hasNext()) {
            output.append('•');
            JsonNode currentCase = endNode.next();
            output.append(processRolessjpPress(currentCase));
            output.append(processOffencessjpPress(currentCase.get("offence"))).append('\n');
        }
        return output.toString();
    }

    /**
     * offences iterator method - handles logic of accused of single or multiple offences and returns output string.
     *
     * @param offencesNode - iterator on offences.
     * @return string with offence data.
     */
    private String processOffencessjpPress(JsonNode offencesNode) {
        StringBuilder outputString = new StringBuilder();
        // below line is due to pmd "avoid using literals in conditional statements" rule.
        boolean offencesNodeSizeBool = offencesNode.size() > 1;
        if (offencesNodeSizeBool) {
            Iterator<JsonNode> offences = offencesNode.elements();
            int counter = 1;
            while (offences.hasNext()) {
                JsonNode thisOffence = offences.next();
                outputString.append("\nOffence ").append(counter).append(": ")
                    .append(thisOffence.get("offenceTitle").asText());
                outputString.append(processReportingRestrictionsjpPress(thisOffence));
                counter += 1;
            }
        } else {
            outputString.append("\nOffence: ").append(offencesNode.get(0).get("offenceTitle").asText())
                .append(processReportingRestrictionsjpPress(offencesNode.get(0)));
        }
        return outputString.toString();
    }

    /**
     * handles reporting restrictions for sjp press.
     *
     * @param node - node which is checked for reporting restriction.
     * @return - text based on whether restriction exists.
     */
    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "(Reporting restriction)";
        }
        return "";
    }

    /**
     * role iteration method for sjp press.
     *
     * @param party - iterator of party.
     * @return list of roles.
     */
    private String processRolessjpPress(JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        String accused = "";
        String postCode = "";
        String prosecutor = "";
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if ("accused".equals(currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT))) {
                String forename = currentParty.get(INDIVIDUAL_DETAILS).get("individualForenames").asText();
                String surname = currentParty.get(INDIVIDUAL_DETAILS).get("individualSurname").asText();
                postCode = currentParty.get(INDIVIDUAL_DETAILS).get("address").get("postCode").asText();
                accused = forename + " " + surname;
            } else {
                prosecutor = currentParty.get("organisationDetails").get("organisationName").asText();
            }
        }
        return "Accused: " + accused + "\nPostcode: " + postCode + "\nProsecutor: " + prosecutor;
    }
}
