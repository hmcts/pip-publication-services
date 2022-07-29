package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.util.StringUtils;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.PartyRoleMapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.TooManyMethods")
public final class DataManipulation {
    private static final String POSTCODE = "postCode";
    private static final String COURT_HOUSE = "courtHouse";
    private static final int MINUTES_PER_HOUR = 60;

    private DataManipulation() {
        throw new UnsupportedOperationException();
    }

    public static List<String> formatVenueAddress(JsonNode artefact) {
        List<String> address = new ArrayList<>();
        JsonNode arrayNode = artefact.get("venue").get("venueAddress").get("line");
        for (JsonNode jsonNode : arrayNode) {
            if (!jsonNode.asText().isEmpty()) {
                address.add(jsonNode.asText());
            }
        }
        if (!Helpers.findAndReturnNodeText(artefact.get("venue").get("venueAddress"), POSTCODE).isEmpty()) {
            address.add(artefact.get("venue").get("venueAddress").get(POSTCODE).asText());
        }
        return address;

    }

    public static void formatCourtAddress(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            StringBuilder formattedCourtAddress = new StringBuilder();

            if (courtList.get(COURT_HOUSE).has("courtHouseAddress")) {
                JsonNode courtHouseAddress = courtList.get(COURT_HOUSE).get("courtHouseAddress");

                Helpers.loopAndFormatString(courtHouseAddress, "line",
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
        if (!Helpers.findAndReturnNodeText(node, nodeName).isEmpty()) {
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
                                ((ObjectNode)hearing).put("applicant", "");
                                ((ObjectNode)hearing).put("respondent", "");
                            }
                            hearing.get("case").forEach(hearingCase -> {
                                manipulateCaseInformation(hearingCase);
                            });
                        });
                    });
                    formattedCourtRoomName(courtRoom, session, formattedJudiciary);
                });
            });
        });
    }

    private static void manipulateCaseInformation(JsonNode hearingCase) {
        if (!Helpers.findAndReturnNodeText(hearingCase, "caseSequenceIndicator").isEmpty()) {
            ((ObjectNode)hearingCase).put("caseName",
                Helpers.findAndReturnNodeText(hearingCase, "caseName")
                    + " [" + hearingCase.get("caseSequenceIndicator").asText() + "]");
        }

        if (!hearingCase.has("caseType")) {
            ((ObjectNode)hearingCase).put("caseType", "");
        }
    }

    private static void findAndManipulatePartyInformation(JsonNode hearing) {
        StringBuilder applicant = new StringBuilder();
        StringBuilder respondent = new StringBuilder();

        hearing.get("party").forEach(party -> {
            if (!Helpers.findAndReturnNodeText(party, "partyRole").isEmpty()) {
                switch (PartyRoleMapper.convertPartyRole(party.get("partyRole").asText())) {
                    case "APPLICANT_PETITIONER": {
                        formatPartyNonRepresentative(party, applicant);
                        break;
                    }
                    case "APPLICANT_PETITIONER_REPRESENTATIVE": {
                        final String applicantPetitionerDetails = createIndividualDetails(party);
                        if (!applicantPetitionerDetails.isEmpty()) {
                            applicant.append("LEGALADVISOR: ");
                            applicant.append(applicantPetitionerDetails + ", ");
                        }
                        break;
                    }
                    case "RESPONDENT": {
                        formatPartyNonRepresentative(party, respondent);
                        break;
                    }
                    case "RESPONDENT_REPRESENTATIVE": {
                        final String respondentDetails = createIndividualDetails(party);
                        if (!respondentDetails.isEmpty()) {
                            respondent.append("LEGALADVISOR: ");
                            respondent.append(respondentDetails + ", ");
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        });

        ((ObjectNode)hearing).put("applicant", Helpers.trimAnyCharacterFromStringEnd(applicant.toString()));
        ((ObjectNode)hearing).put("respondent", Helpers.trimAnyCharacterFromStringEnd(respondent.toString()));
    }

    private static void formatPartyNonRepresentative(JsonNode party, StringBuilder builder) {
        String respondentDetails = createIndividualDetails(party);
        respondentDetails = respondentDetails
            + Helpers.stringDelimiter(respondentDetails, ", ");
        builder.insert(0, respondentDetails);
    }

    private static String createIndividualDetails(JsonNode party) {
        if (party.has("individualDetails")) {
            JsonNode individualDetails = party.get("individualDetails");
            return (Helpers.findAndReturnNodeText(individualDetails, "title") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualForenames") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualMiddleName") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualSurname")).trim();
        }
        return "";
    }

    private static void calculateDuration(JsonNode sitting) {
        ZonedDateTime sittingStart = Helpers.convertStringToBst(sitting.get("sittingStart").asText());
        ZonedDateTime sittingEnd = Helpers.convertStringToBst(sitting.get("sittingEnd").asText());

        double durationAsHours = 0;
        double durationAsMinutes = Helpers.convertTimeToMinutes(sittingStart, sittingEnd);

        if (durationAsMinutes >= MINUTES_PER_HOUR) {
            durationAsHours = Math.floor(durationAsMinutes / MINUTES_PER_HOUR);
            durationAsMinutes = durationAsMinutes - (durationAsHours * MINUTES_PER_HOUR);
        }

        String formattedDuration = Helpers.formatDuration((int) durationAsHours,
            (int) durationAsMinutes);

        ((ObjectNode)sitting).put("formattedDuration", formattedDuration);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String time = dtf.format(sittingStart);

        ((ObjectNode)sitting).put("time", time);
    }

    private static void findAndConcatenateHearingPlatform(JsonNode sitting, JsonNode session) {
        StringBuilder formattedHearingPlatform = new StringBuilder();

        if (sitting.has("channel")) {
            Helpers.loopAndFormatString(sitting, "channel",
                                formattedHearingPlatform, ", ");
        } else if (session.has("sessionChannel")) {
            Helpers.loopAndFormatString(session, "sessionChannel",
                                formattedHearingPlatform, ", ");
        }

        ((ObjectNode)sitting).put("caseHearingChannel",
            Helpers.trimAnyCharacterFromStringEnd(formattedHearingPlatform.toString().trim()));
    }

    private static void formattedCourtRoomName(JsonNode courtRoom, JsonNode session,
                                        StringBuilder formattedJudiciary) {
        if (StringUtils.isBlank(formattedJudiciary.toString())) {
            formattedJudiciary.append(courtRoom.get("courtRoomName").asText());
        } else {
            formattedJudiciary.insert(0, courtRoom.get("courtRoomName").asText() + ": ");
        }

        ((ObjectNode)session).put("formattedSessionCourtRoom",
                                  Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()));
    }

    private static String findAndManipulateJudiciary(JsonNode session) {
        AtomicReference<StringBuilder> formattedJudiciary = new AtomicReference<>(new StringBuilder());
        AtomicReference<Boolean> foundPresiding = new AtomicReference<>(false);

        if (session.has("judiciary")) {
            session.get("judiciary").forEach(judiciary -> {
                if ("true".equals(Helpers.findAndReturnNodeText(judiciary, "isPresiding"))) {
                    formattedJudiciary.set(new StringBuilder());
                    formattedJudiciary.get().append(Helpers.findAndReturnNodeText(judiciary, "johKnownAs"));
                    foundPresiding.set(true);
                } else if (!foundPresiding.get()) {
                    String johKnownAs = Helpers.findAndReturnNodeText(judiciary, "johKnownAs");
                    if (StringUtils.isNotBlank(johKnownAs)) {
                        formattedJudiciary.get()
                            .append(johKnownAs)
                            .append(", ");
                    }
                }
            });

            if (!Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()).isEmpty()) {
                formattedJudiciary.get().insert(0, "Before: ");
            }
        }

        return Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString());
    }
}
