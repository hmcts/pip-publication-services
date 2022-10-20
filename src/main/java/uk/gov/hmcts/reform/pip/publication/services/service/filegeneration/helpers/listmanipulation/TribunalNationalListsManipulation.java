package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.TribunalNationalList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.util.ArrayList;
import java.util.List;

public final class TribunalNationalListsManipulation {
    private TribunalNationalListsManipulation() {
    }

    public static List<TribunalNationalList> processRawListData(JsonNode data, Language language) {
        List<TribunalNationalList> cases = new ArrayList<>();

        data.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(roomSession -> {
                    String hearingDate = DateHelper.formatTimeStampToBst(roomSession.get("sessionStartTime").asText(),
                                                                         language, false, false,
                                                                         "dd MMMM");
                    roomSession.get("sittings").forEach(sitting -> {
                        DateHelper.calculateDuration(sitting, Language.ENGLISH, true);
                        sitting.get("hearing").forEach(hearing -> {
                            String hearingType = hearing.get("hearingType").asText();
                            hearing.get("case").forEach(hearingCase -> {
                                String duration = formatDurationWithCaseSequence(
                                    sitting.get("formattedDuration").asText(), hearingCase
                                );
                                cases.add(new TribunalNationalList(
                                    hearingDate, hearingCase.get("caseName").asText(), duration, hearingType,
                                    courtList.get("courtHouse").get("formattedCourtHouseAddress").asText()
                                ));
                            });
                        });
                    });
                });
            });
        });
        return cases;
    }

    private static String formatDurationWithCaseSequence(String duration, JsonNode hearingCase) {
        return hearingCase.has("caseSequenceIndicator")
            ? duration + " " + hearingCase.get("caseSequenceIndicator").asText()
            : duration;
    }
}
