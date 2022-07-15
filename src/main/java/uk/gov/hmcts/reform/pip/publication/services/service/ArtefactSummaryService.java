package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;

import java.util.Iterator;


@Service
@Slf4j
public class ArtefactSummaryService {

    public String artefactSummary(String payload, ListType listType) throws JsonProcessingException {
        switch (listType) {
            case SJP_PUBLIC_LIST:
                return artefactSummarySJPPublic(payload);
            case SJP_PRESS_LIST:
                return "sjp_press";
            case CIVIL_DAILY_CAUSE_LIST:
                return "civ_cause";
            case FAMILY_DAILY_CAUSE_LIST:
                return "fam_cause";
        }
        return "error";
    }

    public String artefactSummarySJPPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();
        JsonNode node = new ObjectMapper().readTree(payload);
        Iterator<JsonNode> endNode =
            node.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").elements();
        while (endNode.hasNext()) {
            output.append("•");
            JsonNode currentHearing = endNode.next();
            output.append(processRolesSJPPublic(currentHearing));
            String offence = currentHearing.get("hearing").get(0).get("offence").get(0).get("offenceTitle").asText();
            output.append(offence).append("\n");
        }
        return output.toString();
    }

    public String processRolesSJPPublic(JsonNode hearing) {
        StringBuilder outputString = new StringBuilder();
        Iterator<JsonNode> partyNames = hearing.get("hearing").get(0).get("party").elements();
        while (partyNames.hasNext()) {
            JsonNode currentParty = partyNames.next();
            switch (currentParty.get("partyRole").asText()) {
                case "ACCUSED":
                    String forenames = currentParty.get("individualDetails").get("individualForenames").asText();
                    String surname = currentParty.get("individualDetails").get("individualSurname").asText();
                    String postCode = currentParty.get("individualDetails").get("address").get("postCode").asText();
                    outputString.append("Accused: ").append(forenames).append(" ").append(surname).append("\n");
                    outputString.append("Postcode: ").append(postCode).append("\n");
                    break;
                case "PROSECUTOR":
                    outputString.append("Prosecutor: ").append(currentParty.get("organisationDetails").get(
                        "organisationName").asText()).append("\n");
                    break;
            }
        }
        return outputString.toString();
    }
}
