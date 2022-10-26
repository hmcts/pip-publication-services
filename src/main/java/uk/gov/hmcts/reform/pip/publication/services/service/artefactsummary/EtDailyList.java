package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;
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
                                GeneralHelper.appendToStringBuilderWithPrefix(output, "Start Time: ",
                                                                              sitting, "time", "\t•");
                                GeneralHelper.appendToStringBuilderWithPrefix(output, "Duration: ",
                                                                              sitting, "formattedDuration",
                                                                              "\n\t\t");
                                output.append(hearingCase.has("caseSequenceIndicator")
                                                  ? " " + hearingCase.get("caseSequenceIndicator").asText()
                                                  : "");

                                GeneralHelper.appendToStringBuilder(output, "Case Number: ",
                                                                    hearingCase, "caseNumber");

                                GeneralHelper.appendToStringBuilder(output, "Claimant: ",
                                                                    hearing, "claimant");
                                output.append(", Rep: ").append(hearing.get("claimantRepresentative").asText());

                                GeneralHelper.appendToStringBuilder(output, "Respondent: ",
                                                                    hearing, "respondent");
                                output.append(", Rep: ").append(hearing.get("respondentRepresentative").asText());

                                GeneralHelper.appendToStringBuilder(output, "Hearing Type: ",
                                                                    hearing, "hearingType");
                                GeneralHelper.appendToStringBuilder(output, "Jurisdiction: ",
                                                                    hearingCase, "caseType");
                                GeneralHelper.appendToStringBuilder(output, "Hearing Platform: ",
                                                                    sitting, "caseHearingChannel");
                                output.append('\n');
                            });
                        });
                    });
                });
            });
        });
        return output.toString();
    }
}
