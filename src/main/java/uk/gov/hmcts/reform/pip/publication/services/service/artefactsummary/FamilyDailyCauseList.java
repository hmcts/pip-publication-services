package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

@Service
public class FamilyDailyCauseList {
    /**
     * Civil cause list parent method - iterates on courtHouse/courtList - if these need to be shown in further
     * iterations, do it here.
     *
     * @param payload - json body.
     * @return - string for output.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryFamilyDailyCause(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulatedDailyListData(node);

        return this.processFamilyDailyCauseList(node);
    }

    /**
     * court room iteration - cycles through courtrooms and deals with routing for hearing channel, judiciary
     * and sitting methods.
     *
     * @param node - jsonnode of courtrooms.
     * @return string with above-mentioned info.
     */
    private String processFamilyDailyCauseList(JsonNode node) {
        StringBuilder output = new StringBuilder();
        node.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Name of Party(ies) - ");
                                output.append(Helpers.findAndReturnNodeText(hearingCase, "caseName"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Case ID - ");
                                output.append(Helpers.findAndReturnNodeText(hearingCase, "caseNumber"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Hearing Type - ");
                                output.append(Helpers.findAndReturnNodeText(hearing, "hearingType"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Location - ");
                                output.append(Helpers.findAndReturnNodeText(sitting, "caseHearingChannel"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Duration - ");
                                output.append(Helpers.findAndReturnNodeText(sitting, "formattedDuration"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Judge - ");
                                output.append(Helpers.findAndReturnNodeText(session, "formattedSessionCourtRoom"));
                            });
                        });
                    });
                });
            });
        });

        return output.toString();
    }
}
