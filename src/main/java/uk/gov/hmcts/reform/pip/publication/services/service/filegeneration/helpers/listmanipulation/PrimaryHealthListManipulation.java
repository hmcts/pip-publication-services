package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.PrimaryHealthList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.util.ArrayList;
import java.util.List;

public final class PrimaryHealthListManipulation {
    private PrimaryHealthListManipulation() {
    }

    public static List<PrimaryHealthList> processRawListData(JsonNode data, Language language) {
        List<PrimaryHealthList> cases = new ArrayList<>();

        data.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    String hearingDate = DateHelper.formatTimeStampToBst(session.get("sessionStartTime").asText(),
                                                                         language, false, false,
                                                                         "dd MMMM");
                    session.get("sittings").forEach(sitting -> {
                        DateHelper.calculateDuration(sitting, language, true);
                        sitting.get("hearing").forEach(hearing -> {
                            String hearingType = hearing.get("hearingType").asText();
                            hearing.get("case").forEach(hearingCase -> {
                                String duration = formatDurationWithCaseSequence(
                                    sitting.get("formattedDuration").asText(), hearingCase
                                );
                                cases.add(new PrimaryHealthList(
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
