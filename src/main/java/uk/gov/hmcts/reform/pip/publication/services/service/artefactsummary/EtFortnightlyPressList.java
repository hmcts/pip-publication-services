package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.EtFortnightlyPressListHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;

@Service
public class EtFortnightlyPressList {
    public String artefactSummaryEtFortnightlyPressList(String payload) throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulatedDailyListData(node, Language.ENGLISH, false);
        EtFortnightlyPressListHelper.etFortnightlyListFormatted(node, Language.ENGLISH);
        EtFortnightlyPressListHelper.splitByCourtAndDate(node);
        return this.processEtFortnightlyPressList(node);
    }

    public String processEtFortnightlyPressList(JsonNode node) {
        StringBuilder output = new StringBuilder();
        node.get("courtLists").forEach(courtList -> {
            courtList.get("sittings").forEach(sitting -> {
                sitting.get("hearing").forEach(hearings -> {
                    hearings.forEach(hearing -> {
                        hearing.get("case").forEach(hearingCase -> {
                            output.append('\n');
                            GeneralHelper.appendToStringBuilder(output, "Courtroom - ",
                                                                hearing, "courtRoom");
                            GeneralHelper.appendToStringBuilder(output, "Start Time - ",
                                                                hearing, "time");
                            output.append('\n');
                            checkCaseSequenceNo(output, hearing, hearingCase);
                            GeneralHelper.appendToStringBuilder(output, "Case Number - ",
                                                                hearingCase, "caseNumber");
                            GeneralHelper.appendToStringBuilder(output, "Claimant - ",
                                                                hearing,"claimant_petitioner");
                            GeneralHelper.appendToStringBuilder(output, "Respondent - ",
                                                                hearing,"respondent");
                            GeneralHelper.appendToStringBuilder(output, "Hearing Type - ",
                                                                hearing,"hearingType");
                            GeneralHelper.appendToStringBuilder(output, "Jurisdiction - ",
                                                                hearingCase,"caseType");
                            GeneralHelper.appendToStringBuilder(output, "Hearing Platform - ",
                                                                hearing,"caseHearingChannel");
                        });
                    });
                });
            });
        });
        return output.toString();
    }

    private void checkCaseSequenceNo(StringBuilder output, JsonNode hearing, JsonNode hearingCase) {
        String caseSequenceNo = "";
        if (!GeneralHelper.findAndReturnNodeText(hearingCase, "caseSequenceIndicator")
            .isEmpty()) {
            caseSequenceNo = " " + GeneralHelper.findAndReturnNodeText(hearingCase,
                                                                       "caseSequenceIndicator");
        }
        String formattedDuration = "Duration - "
            + GeneralHelper.findAndReturnNodeText(hearing, "formattedDuration")
            + caseSequenceNo;
        output.append(formattedDuration);
    }
}
