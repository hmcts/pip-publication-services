package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Locale;

@Service
public class SjpPressList {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INDIVIDUAL_DETAILS = "individualDetails";

    /**
     * sjp press parent method - iterates over session data. Routes to specific methods which handle offences and
     * judiciary roles.
     *
     * @param payload - json body.
     * @return String with final summary data.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummarySjpPress(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();

        OBJECT_MAPPER.readTree(payload).get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            output
                                .append('•')
                                .append(processRolesSjpPress(hearing))
                                .append(processOffencesSjpPress(hearing.get("offence")))
                                .append('\n');
                        });
                    });
                });
            });
        });

        return output.toString();
    }

    /**
     * offences iterator method - handles logic of accused of single or multiple offences and returns output string.
     *
     * @param offencesNode - iterator on offences.
     * @return string with offence data.
     */
    private String processOffencesSjpPress(JsonNode offencesNode) {
        StringBuilder outputString = new StringBuilder();
        // below line is due to pmd "avoid using literals in conditional statements" rule.
        boolean offencesNodeSizeBool = offencesNode.size() > 1;
        if (offencesNodeSizeBool) {
            Iterator<JsonNode> offences = offencesNode.elements();
            int counter = 1;
            while (offences.hasNext()) {
                JsonNode thisOffence = offences.next();
                outputString
                    .append("\nOffence ")
                    .append(counter)
                    .append(": ")
                    .append(thisOffence.get("offenceTitle").asText())
                    .append(processReportingRestrictionSjpPress(thisOffence));
                counter += 1;
            }
        } else {
            outputString
                .append("\nOffence: ")
                .append(offencesNode.get(0).get("offenceTitle").asText())
                .append(processReportingRestrictionSjpPress(offencesNode.get(0)));
        }
        return outputString.toString();
    }

    /**
     * handles reporting restrictions for sjp press.
     *
     * @param node - node which is checked for reporting restriction.
     * @return - text based on whether restriction exists.
     */
    private String processReportingRestrictionSjpPress(JsonNode node) {
        return node.get("reportingRestriction").asBoolean() ? "(Reporting restriction)" : "";
    }

    /**
     * role iteration method for sjp press.
     *
     * @param hearing - iterator of hearing.
     * @return list of roles.
     */
    private String processRolesSjpPress(JsonNode hearing) {
        Iterator<JsonNode> partyNode = hearing.get("party").elements();
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
