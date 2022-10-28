package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.LocationHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DailyCauseListHelper.preprocessArtefactForThymeLeafConverter;

public final class EtFortnightlyPressListHelper {
    private static final String COURT_ROOM = "courtRoom";
    private static final String SITTING_DATE = "sittingDate";
    private static final String SITTINGS = "sittings";
    private static final String LEGAL_ADVISOR = "Legal Advisor: ";
    private static final String REP = "rep";

    private EtFortnightlyPressListHelper() {
    }

    public static Context preprocessArtefactForEtFortnightlyListThymeLeafConverter(
        JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) {
        Context context;
        context = preprocessArtefactForThymeLeafConverter(artefact, metadata, language, true);
        etFortnightlyListFormatted(artefact, language);
        splitByCourtAndDate(artefact);
        context.setVariable("regionName", metadata.get("regionName"));
        return context;
    }

    public static void splitByCourtAndDate(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode sittingArray = mapper.createArrayNode();
            Set<String> uniqueSittingDate = findUniqueSittingDate(
                courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM));
            String[] uniqueSittingDates = uniqueSittingDate.toArray(new String[0]);
            for (int i = 0; i < uniqueSittingDates.length; i++) {
                int finalI = i;
                ObjectNode sittingNode = mapper.createObjectNode();
                ArrayNode hearingNodeArray = mapper.createArrayNode();
                (sittingNode).put(SITTING_DATE, uniqueSittingDates[finalI]);
                courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM).forEach(courtRoom -> {
                    courtRoom.get("session").forEach(session -> {
                        session.get(SITTINGS).forEach(sitting -> {
                            checkSittingDateAlreadyExists(sitting, uniqueSittingDates,
                                                          hearingNodeArray, finalI);
                        });
                    });
                });
                (sittingNode).putArray("hearing").addAll(hearingNodeArray);
                sittingArray.add(sittingNode);
            }
            ((ObjectNode)courtList).putArray(SITTINGS).addAll(sittingArray);
        });
    }

    private static void checkSittingDateAlreadyExists(JsonNode sitting, String[] uniqueSittingDate,
                                                      ArrayNode hearingNodeArray, Integer sittingDateIndex) {
        String sittingDate = GeneralHelper.findAndReturnNodeText(sitting, SITTING_DATE);
        if (!sittingDate.isEmpty()
            && sittingDate.equals(uniqueSittingDate[sittingDateIndex])) {
            hearingNodeArray.add(sitting.get("hearing"));
        }
    }

    private static Set<String> findUniqueSittingDate(JsonNode courtRooms) {
        Set<String> uniqueDates = new HashSet<>();
        courtRooms.forEach(courtRoom -> {
            courtRoom.get("session").forEach(session -> {
                session.get(SITTINGS).forEach(sitting -> {
                    uniqueDates.add(GeneralHelper.findAndReturnNodeText(sitting, SITTING_DATE));
                });
            });
        });
        return uniqueDates;
    }

    public static void etFortnightlyListFormatted(JsonNode artefact, Map<String, Object> language) {
        artefact.get("courtLists").forEach(courtList -> {
            courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM).forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get(SITTINGS).forEach(sitting -> {
                        String sittingDate = DateHelper.formatTimeStampToBstHavingWeekDay(
                            sitting.get("sittingStart").asText(),
                            "dd MMMM yyyy", Language.ENGLISH);
                        ((ObjectNode)sitting).put(SITTING_DATE, sittingDate);
                        sitting.get("hearing").forEach(hearing -> {
                            formatCaseTime(sitting, hearing);
                            moveTableColumnValuesToHearing(courtRoom, sitting, hearing, language);
                            if (hearing.has("case")) {
                                hearing.get("case").forEach(cases -> {
                                    if (!cases.has("caseSequenceIndicator")) {
                                        ((ObjectNode)cases).put("caseSequenceIndicator", "");
                                    }
                                });
                            }
                        });
                    });
                });
            });
        });
    }

    private static void moveTableColumnValuesToHearing(JsonNode courtRoom, JsonNode sitting,
                                                       JsonNode hearing,
                                                       Map<String, Object> language) {
        ((ObjectNode)hearing).put(COURT_ROOM,
            GeneralHelper.findAndReturnNodeText(courtRoom, "courtRoomName"));
        ((ObjectNode)hearing).put("claimant",
            GeneralHelper.findAndReturnNodeText(hearing,"claimant"));
        ((ObjectNode)hearing).put("claimantRepresentative",
            getClaimantRepresentative(GeneralHelper.findAndReturnNodeText(hearing,"claimantRepresentative"),
            language));
        String respondent = GeneralHelper.findAndReturnNodeText(hearing, "respondent");
        ((ObjectNode)hearing).put("respondent",
            findRespondent(respondent));
        ((ObjectNode)hearing).put("respondentRepresentative",
            findRespondentRepresentative(respondent,
            language));
        ((ObjectNode)hearing).put("formattedDuration",
            GeneralHelper.findAndReturnNodeText(sitting, "formattedDuration"));
        ((ObjectNode)hearing).put("caseHearingChannel",
            GeneralHelper.findAndReturnNodeText(sitting, "caseHearingChannel"));
    }

    private static String getClaimantRepresentative(String claimantRepresentative,
        Map<String, Object> language) {
        if (claimantRepresentative.isEmpty()) {
            return (String) language.get(REP);
        } else {
            return language.get(REP)
                + claimantRepresentative;
        }
    }

    private static String findRespondent(String respondent) {
        if (respondent.indexOf(LEGAL_ADVISOR) > 0) {
            return GeneralHelper.trimAnyCharacterFromStringEnd(
                respondent.substring(0, respondent.indexOf(LEGAL_ADVISOR)));
        }
        return respondent;
    }

    private static String findRespondentRepresentative(String respondentRepresentative,
                                                       Map<String, Object> language) {
        if (respondentRepresentative.indexOf(LEGAL_ADVISOR) > 0) {
            return GeneralHelper.trimAnyCharacterFromStringEnd(
                language.get(REP) + respondentRepresentative.substring(respondentRepresentative
                     .indexOf(LEGAL_ADVISOR) + LEGAL_ADVISOR.length()));
        }
        if (respondentRepresentative.isEmpty()) {
            return (String) language.get(REP);
        }

        return respondentRepresentative;
    }

    private static void formatCaseTime(JsonNode sitting, JsonNode hearing) {
        if (!GeneralHelper.findAndReturnNodeText(sitting, "sittingStart").isEmpty()) {
            ((ObjectNode)hearing).put("time",
                DateHelper.timeStampToBstTimeWithFormat(GeneralHelper
                .findAndReturnNodeText(sitting, "sittingStart"), "h:mma"));
        }
    }
}
