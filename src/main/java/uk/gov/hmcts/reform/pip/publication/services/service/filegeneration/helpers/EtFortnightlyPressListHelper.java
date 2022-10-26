package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DailyCauseListHelper.preprocessArtefactForThymeLeafConverter;

public final class EtFortnightlyPressListHelper {
    public static final String COURT_ROOM = "courtRoom";
    public static final String SITTING_DATE = "sittingDate";
    public static final String SITTINGS = "sittings";

    private EtFortnightlyPressListHelper() {
    }

    public static Context preprocessArtefactForEtFortnightlyListThymeLeafConverter(
        JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) {
        Context context;
        context = preprocessArtefactForThymeLeafConverter(artefact, metadata, language, true);
        etFortnightlyListFormatted(artefact, Language.valueOf(metadata.get("language")));
        splitByCourtAndDate(artefact);
        context.setVariable("regionName", metadata.get("regionName"));
        return context;
    }

    public static void splitByCourtAndDate(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode sittingArray = mapper.createArrayNode();
            List<String> uniqueSittingDate = findUniqueSittingDate(
                courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM));
            for (int i = 0; i <= uniqueSittingDate.size() - 1; i++) {
                int finalI = i;
                ObjectNode sittingNode = mapper.createObjectNode();
                ArrayNode hearingNodeArray = mapper.createArrayNode();
                (sittingNode).put(SITTING_DATE, uniqueSittingDate.get(finalI));
                courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM).forEach(courtRoom -> {
                    courtRoom.get("session").forEach(session -> {
                        session.get(SITTINGS).forEach(sitting -> {
                            checkSittingDateAlreadyExists(sitting, uniqueSittingDate,
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

    private static void checkSittingDateAlreadyExists(JsonNode sitting, List<String> uniqueSittingDate,
                                                      ArrayNode hearingNodeArray, Integer sittingDateIndex) {
        String sittingDate = GeneralHelper.findAndReturnNodeText(sitting, SITTING_DATE);
        if (!sittingDate.isEmpty()
            && sittingDate.equals(uniqueSittingDate.get(sittingDateIndex))) {
            hearingNodeArray.add(sitting.get("hearing"));
        }
    }

    private static List<String> findUniqueSittingDate(JsonNode courtRooms) {
        List<String> uniqueDate = new ArrayList<>();
        courtRooms.forEach(courtRoom -> {
            courtRoom.get("session").forEach(session -> {
                session.get(SITTINGS).forEach(sitting -> {
                    String sittingDate = GeneralHelper.findAndReturnNodeText(sitting, SITTING_DATE);
                    if (!uniqueDate.contains(sittingDate)) {
                        uniqueDate.add(sittingDate);
                    }
                });
            });
        });
        return uniqueDate;
    }

    public static void etFortnightlyListFormatted(JsonNode artefact, Language language) {
        artefact.get("courtLists").forEach(courtList -> {
            courtList.get(LocationHelper.COURT_HOUSE).get(COURT_ROOM).forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get(SITTINGS).forEach(sitting -> {
                        String sittingDate = DateHelper.formatTimeStampToBst(sitting.get("sittingStart").asText(),
                                Language.ENGLISH, false, false);
                        ((ObjectNode)sitting).put(SITTING_DATE, sittingDate);
                        sitting.get("hearing").forEach(hearing -> {
                            formatCaseTime(sitting, hearing);
                            ((ObjectNode)hearing).put(COURT_ROOM,
                                GeneralHelper.findAndReturnNodeText(courtRoom, "courtRoomName"));
                            ((ObjectNode)hearing).put("claimant_petitioner",
                                getClaimantPetitioner(GeneralHelper.findAndReturnNodeText(hearing,
                                "claimant_petitioner"),
                                    GeneralHelper.findAndReturnNodeText(hearing,
                                "claimant_petitioner_representative"),
                                    language));
                            ((ObjectNode)hearing).put("respondent",
                                    formatRespondent(GeneralHelper.findAndReturnNodeText(hearing, "respondent"),
                                    language));
                            ((ObjectNode)hearing).put("formattedDuration",
                                    GeneralHelper.findAndReturnNodeText(sitting, "formattedDuration"));
                            ((ObjectNode)hearing).put("caseHearingChannel",
                                    GeneralHelper.findAndReturnNodeText(sitting, "caseHearingChannel"));

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

    private static String getClaimantPetitioner(String claimant,
                                                String claimantRepresentative, Language language) {
        if (claimantRepresentative.isEmpty()) {
            return formatClaimant(claimant, "", language,
                           "No Representative", "Dim Cynrychiolydd");
        } else {
            return formatClaimant(claimant, claimantRepresentative, language,
                           "Rep: ", "Cynrychiolydd: ");
        }
    }

    private static String formatClaimant(String claimant, String claimantRepresentative, Language language,
                                         String englishText, String welshText) {
        if (claimant.isEmpty()) {
            return
                (language == Language.ENGLISH) ? englishText : welshText
                + claimantRepresentative;
        } else {
            return claimant + ", "
                + ((language == Language.ENGLISH) ? englishText : welshText)
                + claimantRepresentative;
        }
    }

    private static String formatRespondent(String respondent, Language language) {
        String rep = (language == Language.ENGLISH) ?  "Rep" : "Cynrychiolydd";
        return respondent.replace("Legal Advisor", rep);
    }

    private static void formatCaseTime(JsonNode sitting, JsonNode hearing) {
        if (!GeneralHelper.findAndReturnNodeText(sitting, "sittingStart").isEmpty()) {
            ((ObjectNode)hearing).put("time",
                DateHelper.timeStampToBstTimeWithFormat(GeneralHelper
                .findAndReturnNodeText(sitting, "sittingStart"), "h:mma"));
        }
    }
}
