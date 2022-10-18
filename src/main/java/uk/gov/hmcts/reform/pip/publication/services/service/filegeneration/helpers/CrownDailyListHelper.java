package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DailyCauseListHelper.preprocessArtefactForThymeLeafConverter;

public final class CrownDailyListHelper {
    public static final String PROSECUTING_AUTHORITY = "prosecuting_authority";
    public static final String DEFENDANT = "defendant";
    public static final String COURT_LIST = "courtLists";
    public static final String CASE = "case";

    private CrownDailyListHelper() {
    }

    public static Context preprocessArtefactForCrownDailyListThymeLeafConverter(
        JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) {
        Context context;
        context = preprocessArtefactForThymeLeafConverter(artefact, metadata, language);
        manipulatedCrownDailyListData(artefact);
        findUnallocatedCasesInCrownDailyListData(artefact);
        context.setVariable("version", artefact.get("document").get("version").asText());
        return context;
    }

    public static void manipulatedCrownDailyListData(JsonNode artefact) {
        artefact.get(COURT_LIST).forEach(courtList -> {
            courtList.get(LocationHelper.COURT_HOUSE).get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        formatCaseTime(sitting);
                        sitting.get("hearing").forEach(hearing -> {
                            if (hearing.has("party")) {
                                findAndManipulatePartyInformation(hearing);
                            } else {
                                ((ObjectNode) hearing).put(PROSECUTING_AUTHORITY, "");
                                ((ObjectNode) hearing).put(DEFENDANT, "");
                            }
                            formatCaseInformation(hearing);
                            formatCaseHtmlTable(hearing);
                        });
                    });
                });
            });
        });
    }

    public static void findUnallocatedCasesInCrownDailyListData(JsonNode artefact) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode unAllocatedCasesNodeArray = mapper.createArrayNode();
        artefact.get(COURT_LIST).forEach(courtList -> {
            final int[] roomCount = {0};
            courtList.get(LocationHelper.COURT_HOUSE).get("courtRoom").forEach(courtRoom -> {
                if (GeneralHelper.findAndReturnNodeText(courtRoom, "courtRoomName").contains("to be allocated")) {
                    JsonNode cloneCourtRoom = courtRoom.deepCopy();
                    unAllocatedCasesNodeArray.add(cloneCourtRoom);
                    ((ObjectNode)courtRoom).put("exclude", true);
                }
                roomCount[0]++;
            });
        });

        //IF THERE IS ANY UNALLOCATED CASES, ADD THE SECTION AT END OF COURTLIST ARRAY
        if (unAllocatedCasesNodeArray.size() > 0) {
            JsonNode cloneCourtList = artefact.get(COURT_LIST).get(0).deepCopy();
            formatUnallocatedCourtList(cloneCourtList, unAllocatedCasesNodeArray);

            ArrayNode courtListArray = mapper.createArrayNode();

            if (artefact.get(COURT_LIST).isArray()) {
                for (final JsonNode courtList : artefact.get(COURT_LIST)) {
                    ((ObjectNode)courtList).put("unallocatedCases", false);
                    courtListArray.add(courtList);
                }
                courtListArray.add(cloneCourtList);
                ((ObjectNode)artefact).putArray(COURT_LIST).addAll(courtListArray);
            }
        }
    }

    private static void formatUnallocatedCourtList(JsonNode courtListForUnallocatedCases, ArrayNode unallocatedCase) {
        ((ObjectNode)courtListForUnallocatedCases.get(LocationHelper.COURT_HOUSE)).put("courtHouseName", "");
        ((ObjectNode)courtListForUnallocatedCases.get(LocationHelper.COURT_HOUSE)).put("courtHouseAddress", "");
        ((ObjectNode)courtListForUnallocatedCases).put("unallocatedCases", true);
        ((ObjectNode)courtListForUnallocatedCases.get(LocationHelper.COURT_HOUSE))
            .putArray("courtRoom").addAll(unallocatedCase);
    }

    private static void formatCaseInformation(JsonNode hearing) {
        AtomicReference<StringBuilder> linkedCases = new AtomicReference<>(new StringBuilder());
        StringBuilder listingNotes = new StringBuilder();

        if (hearing.has(CASE)) {
            hearing.get(CASE).forEach(cases -> {
                linkedCases.set(new StringBuilder());
                if (cases.has("caseLinked")) {
                    cases.get("caseLinked").forEach(caseLinked -> {
                        linkedCases.get()
                            .append(GeneralHelper.findAndReturnNodeText(caseLinked, "caseId")).append(", ");
                    });
                }
                ((ObjectNode)cases).put("linkedCases",
                                        GeneralHelper.trimAnyCharacterFromStringEnd(linkedCases.toString()));

                if (!cases.has("caseSequenceIndicator")) {
                    ((ObjectNode)cases).put("caseSequenceIndicator", "");
                }
            });
        }

        if (hearing.has("listingDetails")) {
            listingNotes.append(hearing.get("listingDetails").get("listingRepDeadline"));
            listingNotes.append(", ");
        }
        ((ObjectNode)hearing).put("listingNotes", GeneralHelper.trimAnyCharacterFromStringEnd(listingNotes.toString())
            .replace("\"", ""));
    }

    private static void formatCaseHtmlTable(JsonNode hearing) {
        if (hearing.has(CASE)) {
            hearing.get(CASE).forEach(cases -> {
                ((ObjectNode)cases).put("caseCellBorder", "");
                if (!GeneralHelper.findAndReturnNodeText(cases, "linkedCases").isEmpty()
                    || !GeneralHelper.findAndReturnNodeText(hearing, "listingNotes").isEmpty()) {
                    ((ObjectNode)cases).put("caseCellBorder", "no-border-bottom");
                }

                ((ObjectNode)cases).put("linkedCasesBorder", "");
                if (!GeneralHelper.findAndReturnNodeText(cases, "linkedCases").isEmpty()) {
                    ((ObjectNode)cases).put("linkedCasesBorder", "no-border-bottom");
                }
            });
        }
    }

    private static void formatCaseTime(JsonNode sitting) {
        if (!GeneralHelper.findAndReturnNodeText(sitting, "sittingStart").isEmpty()) {
            ((ObjectNode)sitting).put("time",
                DateHelper.timeStampToBstTimeWithFormat(GeneralHelper
                    .findAndReturnNodeText(sitting, "sittingStart"), "h:mma"));
        }
    }

    private static void findAndManipulatePartyInformation(JsonNode hearing) {
        StringBuilder prosecutingAuthority = new StringBuilder();
        StringBuilder defendant = new StringBuilder();

        hearing.get("party").forEach(party -> {
            if (!GeneralHelper.findAndReturnNodeText(party, "partyRole").isEmpty()) {
                switch (PartyRoleMapper.convertPartyRole(party.get("partyRole").asText())) {
                    case "PROSECUTING_AUTHORITY": {
                        formatPartyInformation(party, prosecutingAuthority);
                        break;
                    }
                    case "DEFENDANT": {
                        formatPartyInformation(party, defendant);
                        break;
                    }
                    default:
                        break;
                }
            }
        });

        ((ObjectNode) hearing).put(PROSECUTING_AUTHORITY,
                                   GeneralHelper.trimAnyCharacterFromStringEnd(prosecutingAuthority.toString()));
        ((ObjectNode) hearing).put(DEFENDANT, GeneralHelper.trimAnyCharacterFromStringEnd(defendant.toString()));
    }

    private static void formatPartyInformation(JsonNode party, StringBuilder builder) {
        String partyDetails = createIndividualDetails(party);
        partyDetails = partyDetails
            + GeneralHelper.stringDelimiter(partyDetails, ", ");
        builder.insert(0, partyDetails);
    }

    private static String createIndividualDetails(JsonNode party) {
        if (party.has("individualDetails")) {
            JsonNode individualDetails = party.get("individualDetails");
            String forNames = GeneralHelper.findAndReturnNodeText(individualDetails, "individualForenames");
            String middleName = GeneralHelper.findAndReturnNodeText(individualDetails, "individualMiddleName");
            String separator = " ";
            if (!forNames.isEmpty() || !middleName.isEmpty()) {
                separator = ", ";
            }
            return (GeneralHelper.findAndReturnNodeText(individualDetails, "title") + " "
                + GeneralHelper.findAndReturnNodeText(individualDetails, "individualSurname")
                + separator
                + forNames + " "
                + middleName).trim();
        }
        return "";
    }
}
