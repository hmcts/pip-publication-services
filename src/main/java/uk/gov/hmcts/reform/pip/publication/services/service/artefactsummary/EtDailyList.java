package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.EtDailyListManipulation;

@Service
public class EtDailyList {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String artefactSummaryEtDailyList(String payload) throws JsonProcessingException {
        JsonNode jsonPayload = OBJECT_MAPPER.readTree(payload);
        EtDailyListManipulation.processRawListData(jsonPayload, Language.ENGLISH);

        StringBuilder output = new StringBuilder(140);
        jsonPayload.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                output
                                    .append("\t•Start Time: ")
                                    .append(sitting.get("time").asText())
                                    .append("\n\t\tDuration: ")
                                    .append(sitting.get("formattedDuration").asText())
                                    .append(hearingCase.has("caseSequenceIndicator")
                                                ? " " + hearingCase.get("caseSequenceIndicator").asText()
                                                : "")
                                    .append("\nCase Number: ")
                                    .append(hearingCase.get("caseNumber").asText())
                                    .append("\nClaimant: ")
                                    .append(hearing.get("claimant").asText())
                                    .append(", Rep: ")
                                    .append(hearing.get("claimantRepresentative").asText())
                                    .append("\nRespondent: ")
                                    .append(hearing.get("respondent").asText())
                                    .append(", Rep: ")
                                    .append(hearing.get("respondentRepresentative").asText())
                                    .append("\nHearing Type: ")
                                    .append(hearing.get("hearingType").asText())
                                    .append("\nJurisdiction: ")
                                    .append(hearingCase.has("caseType")
                                                ? hearingCase.get("caseType").asText()
                                                : "")
                                    .append("\nHearing Platform: ")
                                    .append(sitting.get("caseHearingChannel").asText())
                                    .append('\n');
                            });
                        });
                    });
                });
            });
        });
        return output.toString();
    }
}
