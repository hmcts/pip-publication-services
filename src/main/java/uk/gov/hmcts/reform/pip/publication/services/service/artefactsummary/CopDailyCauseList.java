package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.GeneralHelper;

@Service
public class CopDailyCauseList {

    /**
     * COP Daily Cause List summary producer.
     *
     * @param payload - The artefact.
     * @return - The returned summary for the list.
     * @throws JsonProcessingException - Thrown if there has been an error while processing the JSON payload.
     */
    public String artefactSummaryCopDailyCauseList(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulateCopListData(node, Language.ENGLISH);

        return this.processCopDailyCauseList(node);
    }

    /**
     * Loops through the artefact and creates the summary.
     *
     * @param node - The artefact to process
     * @return String containing the summary for the list.
     */
    private String processCopDailyCauseList(JsonNode node) {
        StringBuilder output = new StringBuilder(100);
        node.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                output
                                    .append("\n\nName of Party(ies) - ")
                                    .append(GeneralHelper.findAndReturnNodeText(hearingCase, "caseSuppressionName"))
                                    .append("\nCase ID - ")
                                    .append(GeneralHelper.findAndReturnNodeText(hearingCase, "caseNumber"))
                                    .append("\nHearing Type - ")
                                    .append(GeneralHelper.findAndReturnNodeText(hearing, "hearingType"))
                                    .append("\nLocation - ")
                                    .append(GeneralHelper.findAndReturnNodeText(sitting, "caseHearingChannel"))
                                    .append("\nDuration - ")
                                    .append(GeneralHelper.findAndReturnNodeText(sitting, "formattedDuration"))
                                    .append(' ')
                                    .append(GeneralHelper.findAndReturnNodeText(hearingCase, "caseIndicator"))
                                    .append("\nBefore Hon - ")
                                    .append(GeneralHelper.findAndReturnNodeText(session, "formattedSessionJoh"));
                            });
                        });
                    });
                });
            });
        });

        return output.toString();
    }
}
