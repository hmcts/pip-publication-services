package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.util.StringUtils;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtRoom;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Hearing;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.Sitting;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public final class DataManipulation {
    private static final String POSTCODE = "postCode";
    private static final String COURT_HOUSE = "courtHouse";
    private static final int MINUTES_PER_HOUR = 60;
    private static final String CASE_SEQUENCE_INDICATOR = "caseSequenceIndicator";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String APPLICANT = "applicant";
    public static final String RESPONDENT = "respondent";
    public static final String CHANNEL = "channel";
    public static final String JUDICIARY = "judiciary";

    private DataManipulation() {
        throw new UnsupportedOperationException();
    }

    public static void manipulateCaseInformationForCop(JsonNode hearingCase) {
        if (!GeneralHelper.findAndReturnNodeText(hearingCase, CASE_SEQUENCE_INDICATOR).isEmpty()) {
            ((ObjectNode)hearingCase).put("caseIndicator",
                                          "[" + hearingCase.get(CASE_SEQUENCE_INDICATOR).asText() + "]");
        }
    }

    public static void manipulateCopListData(JsonNode artefact) {
        formatRegionName(artefact);
        formatRegionalJoh(artefact);

        artefact.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    ((ObjectNode)session).put("formattedSessionJoh",
                                              DataManipulation.findAndManipulateJudiciaryForCop(session));
                    session.get("sittings").forEach(sitting -> {
                        DataManipulation.calculateDuration(sitting);
                        DataManipulation.findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(DataManipulation::manipulateCaseInformationForCop);
                        });
                    });
                });
            });
        });
    }

    public static List<String> formatVenueAddress(JsonNode artefact) {
        List<String> address = new ArrayList<>();
        JsonNode arrayNode = artefact.get("venue").get("venueAddress").get("line");
        for (JsonNode jsonNode : arrayNode) {
            if (!jsonNode.asText().isEmpty()) {
                address.add(jsonNode.asText());
            }
        }
        if (!GeneralHelper.findAndReturnNodeText(artefact.get("venue").get("venueAddress"), POSTCODE).isEmpty()) {
            address.add(artefact.get("venue").get("venueAddress").get(POSTCODE).asText());
        }
        return address;

    }

    public static void formatCourtAddress(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            StringBuilder formattedCourtAddress = new StringBuilder();

            if (courtList.get(COURT_HOUSE).has("courtHouseAddress")) {
                JsonNode courtHouseAddress = courtList.get(COURT_HOUSE).get("courtHouseAddress");

                GeneralHelper.loopAndFormatString(courtHouseAddress, "line",
                                            formattedCourtAddress, "|");

                checkAndFormatAddress(courtHouseAddress, "town",
                                           formattedCourtAddress, '|');

                checkAndFormatAddress(courtHouseAddress, "county",
                                           formattedCourtAddress, '|');

                checkAndFormatAddress(courtHouseAddress, POSTCODE,
                                           formattedCourtAddress, '|');
            }

            ((ObjectNode)courtList.get(COURT_HOUSE)).put("formattedCourtHouseAddress",
                formattedCourtAddress.toString().replaceAll(", $", ""));
        });
    }

    private static void checkAndFormatAddress(JsonNode node, String nodeName,
                                      StringBuilder builder, Character delimiter) {
        if (!GeneralHelper.findAndReturnNodeText(node, nodeName).isEmpty()) {
            builder
                .append(node.get(nodeName).asText())
                .append(delimiter);
        }
    }

    public static void manipulatedDailyListData(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            courtList.get(COURT_HOUSE).get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    StringBuilder formattedJudiciary = new StringBuilder();
                    formattedJudiciary.append(findAndManipulateJudiciary(session));
                    session.get("sittings").forEach(sitting -> {
                        calculateDuration(sitting);
                        findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get("hearing").forEach(hearing -> {
                            if (hearing.has("party")) {
                                findAndManipulatePartyInformation(hearing);
                            } else {
                                ((ObjectNode)hearing).put(APPLICANT, "");
                                ((ObjectNode)hearing).put(RESPONDENT, "");
                            }
                            hearing.get("case").forEach(DataManipulation::manipulateCaseInformation);
                        });
                    });
                    formattedCourtRoomName(courtRoom, session, formattedJudiciary);
                });
            });
        });
    }

    public static void calculateDuration(JsonNode sitting) {
        ZonedDateTime sittingStart = DateHelper.convertStringToUtc(sitting.get("sittingStart").asText());
        ZonedDateTime sittingEnd = DateHelper.convertStringToUtc(sitting.get("sittingEnd").asText());

        double durationAsHours = 0;
        double durationAsMinutes = DateHelper.convertTimeToMinutes(sittingStart, sittingEnd);

        if (durationAsMinutes >= MINUTES_PER_HOUR) {
            durationAsHours = Math.floor(durationAsMinutes / MINUTES_PER_HOUR);
            durationAsMinutes = durationAsMinutes - (durationAsHours * MINUTES_PER_HOUR);
        }

        String formattedDuration = DateHelper.formatDuration((int) durationAsHours,
            (int) durationAsMinutes);

        ((ObjectNode)sitting).put("formattedDuration", formattedDuration);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String time = dtf.format(sittingStart);

        ((ObjectNode)sitting).put("time", time);
    }

    private static void manipulateCaseInformation(JsonNode hearingCase) {
        if (!GeneralHelper.findAndReturnNodeText(hearingCase, CASE_SEQUENCE_INDICATOR).isEmpty()) {
            ((ObjectNode)hearingCase).put("caseName",
                GeneralHelper.findAndReturnNodeText(hearingCase, "caseName")
                    + " [" + hearingCase.get(CASE_SEQUENCE_INDICATOR).asText() + "]");
        }

        if (!hearingCase.has("caseType")) {
            ((ObjectNode)hearingCase).put("caseType", "");
        }
    }

    private static void findAndManipulatePartyInformation(JsonNode hearing) {
        StringBuilder applicant = new StringBuilder();
        StringBuilder respondent = new StringBuilder();

        hearing.get("party").forEach(party -> {
            if (!GeneralHelper.findAndReturnNodeText(party, "partyRole").isEmpty()) {
                switch (PartyRoleMapper.convertPartyRole(party.get("partyRole").asText())) {
                    case "APPLICANT_PETITIONER": {
                        formatPartyNonRepresentative(party, applicant);
                        break;
                    }
                    case "APPLICANT_PETITIONER_REPRESENTATIVE": {
                        final String applicantPetitionerDetails = createIndividualDetails(party);
                        if (!applicantPetitionerDetails.isEmpty()) {
                            applicant.append("Legal Advisor: ");
                            applicant.append(applicantPetitionerDetails).append(", ");
                        }
                        break;
                    }
                    case RESPONDENT: {
                        formatPartyNonRepresentative(party, respondent);
                        break;
                    }
                    case "RESPONDENT_REPRESENTATIVE": {
                        final String respondentDetails = createIndividualDetails(party);
                        if (!respondentDetails.isEmpty()) {
                            respondent.append("Legal Advisor: ");
                            respondent.append(respondentDetails).append(", ");
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        });

        ((ObjectNode)hearing).put(APPLICANT, GeneralHelper.trimAnyCharacterFromStringEnd(applicant.toString()));
        ((ObjectNode)hearing).put(RESPONDENT, GeneralHelper.trimAnyCharacterFromStringEnd(respondent.toString()));
    }

    private static void formatPartyNonRepresentative(JsonNode party, StringBuilder builder) {
        String respondentDetails = createIndividualDetails(party);
        respondentDetails = respondentDetails
            + GeneralHelper.stringDelimiter(respondentDetails, ", ");
        builder.insert(0, respondentDetails);
    }

    private static String createIndividualDetails(JsonNode party) {
        if (party.has("individualDetails")) {
            JsonNode individualDetails = party.get("individualDetails");
            return (GeneralHelper.findAndReturnNodeText(individualDetails, "title") + " "
                + GeneralHelper.findAndReturnNodeText(individualDetails, "individualForenames") + " "
                + GeneralHelper.findAndReturnNodeText(individualDetails, "individualMiddleName") + " "
                + GeneralHelper.findAndReturnNodeText(individualDetails, "individualSurname")).trim();
        }
        return "";
    }

    private static void findAndConcatenateHearingPlatform(JsonNode sitting, JsonNode session) {
        StringBuilder formattedHearingPlatform = new StringBuilder();

        if (sitting.has(CHANNEL)) {
            GeneralHelper.loopAndFormatString(sitting, CHANNEL,
                                              formattedHearingPlatform, ", ");
        } else if (session.has("sessionChannel")) {
            GeneralHelper.loopAndFormatString(session, "sessionChannel",
                                              formattedHearingPlatform, ", ");
        }

        ((ObjectNode)sitting).put("caseHearingChannel", GeneralHelper.trimAnyCharacterFromStringEnd(
            formattedHearingPlatform.toString().trim()));
    }

    public static String findAndManipulateJudiciaryForCop(JsonNode session) {
        StringBuilder formattedJudiciary = new StringBuilder();

        try {
            session.get(JUDICIARY).forEach(judiciary -> {
                if (formattedJudiciary.length() != 0) {
                    formattedJudiciary.append(", ");
                }

                formattedJudiciary.append(GeneralHelper.findAndReturnNodeText(judiciary, "johTitle"));
                formattedJudiciary.append(' ');
                formattedJudiciary.append(GeneralHelper.findAndReturnNodeText(judiciary, "johNameSurname"));
            });

        } catch (Exception ignored) {
            //No catch required, this is a valid scenario and makes the code cleaner than many if statements
        }

        return GeneralHelper.trimAnyCharacterFromStringEnd(formattedJudiciary.toString());
    }

    public static void formatRegionName(JsonNode artefact) {
        try {
            ((ObjectNode) artefact).put("regionName",
                                        artefact.get("locationDetails").get("region").get("name").asText());
        } catch (Exception e) {
            ((ObjectNode) artefact).put("regionName", "");
        }
    }

    public static void formatRegionalJoh(JsonNode artefact) {
        StringBuilder formattedJoh = new StringBuilder();
        try {
            artefact.get("locationDetails").get("region").get("regionalJOH").forEach(joh -> {
                if (formattedJoh.length() != 0) {
                    formattedJoh.append(", ");
                }

                formattedJoh.append(GeneralHelper.findAndReturnNodeText(joh, "johKnownAs"));
                formattedJoh.append(' ');
                formattedJoh.append(GeneralHelper.findAndReturnNodeText(joh, "johNameSurname"));
            });

            ((ObjectNode) artefact).put("regionalJoh", formattedJoh.toString());
        } catch (Exception e) {
            ((ObjectNode) artefact).put("regionalJoh", "");
        }
    }

    private static void formattedCourtRoomName(JsonNode courtRoom, JsonNode session,
                                        StringBuilder formattedJudiciary) {
        if (StringUtils.isBlank(formattedJudiciary.toString())) {
            formattedJudiciary.append(courtRoom.get("courtRoomName").asText());
        } else {
            formattedJudiciary.insert(0, courtRoom.get("courtRoomName").asText() + ": ");
        }

        ((ObjectNode)session).put("formattedSessionCourtRoom",
            GeneralHelper.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()));
    }

    private static String findAndManipulateJudiciary(JsonNode session) {
        AtomicReference<StringBuilder> formattedJudiciary = new AtomicReference<>(new StringBuilder());
        AtomicReference<Boolean> foundPresiding = new AtomicReference<>(false);

        if (session.has(JUDICIARY)) {
            session.get(JUDICIARY).forEach(judiciary -> {
                if ("true".equals(GeneralHelper.findAndReturnNodeText(judiciary, "isPresiding"))) {
                    formattedJudiciary.set(new StringBuilder());
                    formattedJudiciary.get().append(GeneralHelper.findAndReturnNodeText(judiciary, "johKnownAs"));
                    foundPresiding.set(true);
                } else if (!foundPresiding.get()) {
                    String johKnownAs = GeneralHelper.findAndReturnNodeText(judiciary, "johKnownAs");
                    if (StringUtils.isNotBlank(johKnownAs)) {
                        formattedJudiciary.get()
                            .append(johKnownAs)
                            .append(", ");
                    }
                }
            });

            if (!GeneralHelper.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()).isEmpty()) {
                formattedJudiciary.get().insert(0, "Before: ");
            }
        }

        return GeneralHelper.trimAnyCharacterFromStringEnd(formattedJudiciary.toString());
    }

    private static void handlePartiesScss(JsonNode node, Hearing hearing) {
        Map<String, String> parties = new ConcurrentHashMap<>();
        for (JsonNode party : node) {
            switch (party.get("partyRole").asText()) {
                case "APPLICANT_PETITIONER":
                    parties.put(APPLICANT, individualDetails(party));
                    break;
                case "APPLICANT_PETITIONER_REPRESENTATIVE":
                    parties.put("applicantRepresentative", individualDetails(party));
                    break;
                case RESPONDENT:
                    parties.put(RESPONDENT, individualDetails(party));
                    break;
                case "RESPONDENT_REPRESENTATIVE":
                    parties.put("respondentRepresentative", individualDetails(party));
                    break;
                default:
                    break;
            }
            hearing.setAppellant(parties.get(APPLICANT) + ",\nLegal Advisor: " + parties.get(
                "applicantRepresentative"));
            hearing.setRespondent(parties.get(RESPONDENT) + ",\nLegal Advisor: " + parties.get(
                "respondentRepresentative"));
        }
    }

    private static Hearing hearingBuilder(JsonNode hearingNode) {
        Hearing currentHearing = new Hearing();
        handlePartiesScss(hearingNode.get("party"), currentHearing);
        currentHearing.setRespondent(dealWithInformants(hearingNode));
        currentHearing.setAppealRef(GeneralHelper.safeGet("case.0.caseNumber", hearingNode));
        return currentHearing;
    }

    private static String dealWithInformants(JsonNode node) {
        List<String> informants = new ArrayList<>();
        GeneralHelper.safeGetNode("informant.0.prosecutionAuthorityRef", node).forEach(informant -> {
            informants.add(informant.asText());
        });
        return String.join(", ", informants);
    }

    private static String individualDetails(JsonNode node) {
        List<String> listOfRetrievedData = new ArrayList<>();
        String[] possibleFields = {"title", "individualForenames", "individualMiddleName", "individualSurname"};
        for (String field : possibleFields) {
            Optional<String> detail = Optional.ofNullable(node.get("individualDetails").findValue(field))
                .map(JsonNode::asText)
                .filter(s -> !s.isEmpty());
            detail.ifPresent(listOfRetrievedData::add);
        }
        return String.join(" ", listOfRetrievedData);
    }

    private static Sitting sscsSittingBuilder(String sessionChannel, JsonNode node, String judiciary)
        throws JsonProcessingException {
        Sitting sitting = new Sitting();
        String sittingStart = DateHelper.timeStampToBstTime(GeneralHelper.safeGet("sittingStart", node));
        sitting.setJudiciary(judiciary);
        List<Hearing> listOfHearings = new ArrayList<>();
        if (node.has(CHANNEL)) {
            List<String> channelList = MAPPER.readValue(
                node.get(CHANNEL).toString(), new TypeReference<>() {
                });
            sitting.setChannel(String.join(", ", channelList));
        } else {
            sitting.setChannel(sessionChannel);
        }
        Iterator<JsonNode> nodeIterator = node.get("hearing").elements();
        while (nodeIterator.hasNext()) {
            JsonNode currentHearingNode = nodeIterator.next();
            Hearing currentHearing = hearingBuilder(currentHearingNode);
            currentHearing.setHearingTime(sittingStart);
            listOfHearings.add(currentHearing);
            currentHearing.setJudiciary(sitting.getJudiciary());
        }
        sitting.setListOfHearings(listOfHearings);
        return sitting;
    }

    /**
     * Format the judiciary into a comma seperated string.
     *
     * @param session The session containing the judiciary.
     * @return A string of the formatted judiciary.
     */
    private static String scssFormatJudiciary(JsonNode session) {
        StringBuilder formattedJudiciaryBuilder = new StringBuilder();
        session.get(JUDICIARY).forEach(judiciary -> {
            if (formattedJudiciaryBuilder.length() > 0) {
                formattedJudiciaryBuilder.append(", ");
            }
            formattedJudiciaryBuilder
                .append(GeneralHelper.safeGet("johTitle", judiciary))
                .append(' ').append(GeneralHelper.safeGet("johNameSurname", judiciary));
        });
        return formattedJudiciaryBuilder.toString();
    }

    private static CourtRoom scssCourtRoomBuilder(JsonNode node) throws JsonProcessingException {
        CourtRoom thisCourtRoom = new CourtRoom();
        thisCourtRoom.setName(GeneralHelper.safeGet("courtRoomName", node));
        List<Sitting> sittingList = new ArrayList<>();
        List<String> sessionChannel;
        TypeReference<List<String>> typeReference = new TypeReference<>() {};
        for (final JsonNode session : node.get("session")) {
            sessionChannel = MAPPER.readValue(
                session.get("sessionChannel").toString(),
                typeReference
            );
            String judiciary = scssFormatJudiciary(session);
            String sessionChannelString = String.join(", ", sessionChannel);
            for (JsonNode sitting : session.get("sittings")) {
                sittingList.add(sscsSittingBuilder(sessionChannelString, sitting, judiciary));
            }
        }
        thisCourtRoom.setListOfSittings(sittingList);
        return thisCourtRoom;
    }

    public static CourtHouse courtHouseBuilder(JsonNode node) throws JsonProcessingException {
        JsonNode thisCourtHouseNode = node.get("courtHouse");
        CourtHouse thisCourtHouse = new CourtHouse();
        thisCourtHouse.setName(GeneralHelper.safeGet("courtHouseName", thisCourtHouseNode));
        thisCourtHouse.setPhone(GeneralHelper.safeGet("courtHouseContact.venueTelephone", thisCourtHouseNode));
        thisCourtHouse.setEmail(GeneralHelper.safeGet("courtHouseContact.venueEmail", thisCourtHouseNode));
        List<CourtRoom> courtRoomList = new ArrayList<>();
        for (JsonNode courtRoom : thisCourtHouseNode.get("courtRoom")) {
            courtRoomList.add(scssCourtRoomBuilder(courtRoom));
        }
        thisCourtHouse.setListOfCourtRooms(courtRoomList);
        return thisCourtHouse;
    }
}