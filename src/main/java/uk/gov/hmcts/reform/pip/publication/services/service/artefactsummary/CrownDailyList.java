package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.CrownDailyListHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;

@Service
public class CrownDailyList {
    /**
     * Crown daily list parent method - iterates on courtHouse/courtList - if these need to be shown in further
     * iterations, do it here.
     *
     * @param payload - json body.
     * @return - string for output.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryCrownDailyList(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulatedDailyListData(node, Language.ENGLISH, false);
        CrownDailyListHelper.manipulatedCrownDailyListData(node);
        CrownDailyListHelper.findUnallocatedCasesInCrownDailyListData(node);

        return this.processCrownDailyList(node);
    }

    public String processCrownDailyList(JsonNode node) {
        StringBuilder output = new StringBuilder();
        node.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                output.append('\n');
                                GeneralHelper.appendToStringBuilder(output, "Sitting at - ",
                                                                    sitting, "time");
                                GeneralHelper.appendToStringBuilder(output, "Case Reference - ",
                                                                    hearingCase, "caseNumber");
                                GeneralHelper.appendToStringBuilder(output, "Defendant Name(s) - ",
                                                                    hearing, "defendant");
                                GeneralHelper.appendToStringBuilder(output, "Hearing Type - ",
                                                                    hearing, "hearingType");
                                output.append('\n');
                                checkCaseSequenceNo(output, sitting, hearingCase);
                                GeneralHelper.appendToStringBuilder(output, "Prosecuting Authority - ",
                                                                    hearing,"prosecuting_authority");
                                if (!GeneralHelper.findAndReturnNodeText(hearingCase, "linkedCases").isEmpty()) {
                                    GeneralHelper.appendToStringBuilder(output, "Linked Cases - ",
                                                                        hearingCase, "linkedCases"
                                    );
                                }
                                if (!GeneralHelper.findAndReturnNodeText(hearing, "listingNotes").isEmpty()) {
                                    GeneralHelper.appendToStringBuilder(output, "Listing Notes - ",
                                                                        hearing, "listingNotes"
                                    );
                                }
                            });
                        });
                    });
                });
            });
        });

        return output.toString();
    }

    private void checkCaseSequenceNo(StringBuilder output, JsonNode sitting, JsonNode hearingCase) {
        String caseSequenceNo = "";
        if (!GeneralHelper.findAndReturnNodeText(hearingCase, "linkedCases")
            .isEmpty()) {
            caseSequenceNo = " " + GeneralHelper.findAndReturnNodeText(hearingCase,
                                                                       "caseSequenceIndicator");
        }
        String formattedDuration = "Duration - "
            + GeneralHelper.findAndReturnNodeText(sitting, "formattedDuration")
            + caseSequenceNo;
        output.append(formattedDuration);
    }
}
