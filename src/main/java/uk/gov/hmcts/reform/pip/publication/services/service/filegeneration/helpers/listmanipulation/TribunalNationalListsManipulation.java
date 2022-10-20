package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.TribunalNationalList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.LocationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TribunalNationalListsManipulation {
    private TribunalNationalListsManipulation() {
    }

    public static Context preprocessArtefactForTribunalNationalListsThymeLeafConverter(
        JsonNode artefact, Map<String, String> metadata, Map<String, Object> languageResources) {
        Context context = new Context();
        Language language = Language.valueOf(metadata.get("language"));
        String publicationDate = artefact.get("document").get("publicationDate").asText();
        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               true, false));
        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("email", artefact.get("venue").get("venueContact").get("venueEmail").asText());
        context.setVariable("i18n", languageResources);

        LocationHelper.formatCourtAddress(artefact, "\n", true);

        context.setVariable("cases", TribunalNationalListsManipulation.processRawListData(artefact, language));
        return context;
    }

    public static List<TribunalNationalList> processRawListData(JsonNode data, Language language) {
        List<TribunalNationalList> cases = new ArrayList<>();

        data.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    String hearingDate = DateHelper.formatTimeStampToBst(session.get("sessionStartTime").asText(),
                                                                         language, false, false,
                                                                         "dd MMMM");
                    session.get("sittings").forEach(sitting -> {
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
