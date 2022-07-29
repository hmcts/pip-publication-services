package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.Helpers;

@Service
public class CopDailyCauseList {

    /**
     * COP Daily Cause List summary producer.
     *
     * @param payload - The artefact.
     * @return - The returned summary for the list.
     * @throws JsonProcessingException - Thrown if there has been an error while processing the JSON payload.
     */
    public String createCopDailyCauseListSummary(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulateCopListData(node);

        return this.processCopDailyCauseList(node);
    }

    /**
     * Loops through the artefact and creates the summary.
     *
     * @param node - The artefact to process
     * @return String containing the summary for the list.
     */
    private String processCopDailyCauseList(JsonNode node) {
        StringBuilder output = new StringBuilder();
        node.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                output.append('\n').append('\n')
                                    .append("Name of Party(ies) - ")
                                    .append(Helpers.findAndReturnNodeText(hearingCase, "caseSupressionName"))
                                    .append('\n')
                                    .append("Case ID - ")
                                    .append(Helpers.findAndReturnNodeText(hearingCase, "caseNumber"))
                                    .append('\n').append('\n')
                                    .append("Hearing Type - ")
                                    .append(Helpers.findAndReturnNodeText(hearing, "hearingType"))
                                    .append('\n')
                                    .append("Location - ")
                                    .append(Helpers.findAndReturnNodeText(sitting, "caseHearingChannel"))
                                    .append('\n')
                                    .append("Duration - ")
                                    .append(Helpers.findAndReturnNodeText(sitting, "formattedDuration"))
                                    .append('\n')
                                    .append("Before Hon - ")
                                    .append(Helpers.findAndReturnNodeText(session, "formattedSessionJoh"));
                            });
                        });
                    });
                });
            });
        });

        return output.toString();
    }
}
