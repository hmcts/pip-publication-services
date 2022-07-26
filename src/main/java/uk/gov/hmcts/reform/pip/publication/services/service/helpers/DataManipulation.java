package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class DataManipulation {

    private DataManipulation() {
        throw new UnsupportedOperationException();
    }

    public static List<String> formatVenueAddress(JsonNode artefact) {
        List<String> address = new ArrayList<String>();
        JsonNode arrayNode = artefact.get("venue").get("venueAddress").get("line");
        for (JsonNode jsonNode : arrayNode) {
            if (!jsonNode.asText().isEmpty()) {
                address.add(jsonNode.asText());
            }
        }
        if (!artefact.get("venue").get("venueAddress").get("postCode").asText().isEmpty()) {
            address.add(artefact.get("venue").get("venueAddress").get("postCode").asText());
        }
        return address;

    }

    public static void formatCourtAddress(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            StringBuilder formattedCourtAddress = new StringBuilder();

            JsonNode courtHouseAddress = courtList.get("courtHouse").get("courtHouseAddress");
            courtHouseAddress.get("line").forEach(line -> {
                if (!line.asText().isEmpty()) {
                    formattedCourtAddress
                        .append(line.asText())
                        .append('|');
                }
            });

            if (courtHouseAddress.has("town")) {
                formattedCourtAddress
                    .append(courtHouseAddress.get("town").asText())
                    .append('|');
            }
            if (courtHouseAddress.has("county")) {
                formattedCourtAddress
                    .append(courtHouseAddress.get("county").asText())
                    .append('|');
            }
            if (courtHouseAddress.has("postCode")) {
                formattedCourtAddress
                    .append(courtHouseAddress.get("postCode").asText())
                    .append('|');
            }

            ((ObjectNode)courtList.get("courtHouse")).put("formattedCourtHouseAddress",
                                                          formattedCourtAddress.toString().replaceAll(", $", ""));
        });
    }

    public static void manipulatedDailyListData(JsonNode artefact) {
        artefact.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    StringBuilder formattedJudiciary = new StringBuilder();
                    formattedJudiciary.append(findAndManipulateJudiciary(session));
                    session.get("sittings").forEach(sitting -> {
                        calculateDuration(sitting);
                        findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get("hearing").forEach(hearing -> {
                            findAndManipulatePartyInformation(hearing);
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
        if (hearingCase.has("caseSequenceIndicator")) {
            ((ObjectNode)hearingCase).put("caseName",
                "[" + Helpers.findAndReturnNodeText(hearingCase, "caseName") + "]");
        }

        if (!hearingCase.has("caseType")) {
            ((ObjectNode)hearingCase).put("caseType", "");
        }
    }

    private static void findAndManipulatePartyInformation(JsonNode hearing) {
        StringBuilder applicant = new StringBuilder();
        StringBuilder respondent = new StringBuilder();

        if (hearing.has("party")) {
            hearing.get("party").forEach(party -> {
                switch (convertPartyRole(party.get("partyRole").asText())) {
                    case "APPLICANT_PETITIONER": {
                        applicant.append(createIndividualDetails(party));
                        applicant.append(Helpers.stringDelimiter(applicant.toString(), ", "));
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
                        respondent.append(createIndividualDetails(party));
                        respondent.append(Helpers.stringDelimiter(applicant.toString(), ", "));
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
                        respondent.append("");
                        applicant.append("");
                }
            });

            ((ObjectNode)hearing).put("applicant", Helpers.trimAnyCharacterFromStringEnd(applicant.toString()));
            ((ObjectNode)hearing).put("respondent", Helpers.trimAnyCharacterFromStringEnd(respondent.toString()));

        } else {
            ((ObjectNode)hearing).put("applicant", "");
            ((ObjectNode)hearing).put("respondent", "");
        }
    }

    private static String createIndividualDetails(JsonNode party) {
        if (party.has("individualDetails")) {
            JsonNode individualDetails = party.get("individualDetails");
            return (Helpers.findAndReturnNodeText(individualDetails, "title") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualForenames") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualMiddleName") + " "
                + Helpers.findAndReturnNodeText(individualDetails, "individualSurname")).trim();
        } else {
            return "";
        }
    }

    private static String convertPartyRole(String nonConvertedPartyRole) {
        Map<String, List<String>> partyRoleMappings = new HashMap<>();

        partyRoleMappings.put("APPLICANT_PETITIONER",
                              List.of("APL", "APP", "CLP20", "CRED", "OTH", "PET"));

        partyRoleMappings.put("APPLICANT_PETITIONER_REPRESENTATIVE",
                              List.of("CREP", "CREP20"));

        partyRoleMappings.put("RESPONDENT",
                              List.of("DEBT", "DEF", "DEF20", "RES"));

        partyRoleMappings.put("RESPONDENT_REPRESENTATIVE",
                              List.of("DREP", "DREP20", "RREP"));

        for (Map.Entry<String, List<String>> partyRole : partyRoleMappings.entrySet()) {
            if (partyRole.getKey().equals(nonConvertedPartyRole)
                || partyRole.getValue().contains(nonConvertedPartyRole)) {
                return partyRole.getKey();
            }
        }

        return  "";
    }

    private static void calculateDuration(JsonNode sitting) {
        if (!Helpers.findAndReturnNodeText(sitting,"sittingStart").isEmpty()
            && !Helpers.findAndReturnNodeText(sitting,"sittingEnd").isEmpty()) {
            ZonedDateTime sittingStart = Helpers.convertStringToUtc(sitting.get("sittingStart").asText());
            ZonedDateTime sittingEnd = Helpers.convertStringToUtc(sitting.get("sittingEnd").asText());

            double durationAsHours = 0;
            double durationAsMinutes = Helpers.convertTimeToMinutes(sittingStart, sittingEnd);

            if (durationAsMinutes >= 60) {
                durationAsHours = Math.floor(durationAsMinutes / 60);
                durationAsMinutes = durationAsMinutes - (durationAsHours * 60);
            }

            String formattedDuration = Helpers.formatDuration((int) durationAsHours,
                                                           (int) durationAsMinutes);

            ((ObjectNode)sitting).put("formattedDuration", formattedDuration);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            String time = dtf.format(sittingStart);

            ((ObjectNode)sitting).put("time", time);
        }
    }

    private static void findAndConcatenateHearingPlatform(JsonNode sitting, JsonNode session) {
        StringBuilder formattedHearingPlatform = new StringBuilder();
        if (sitting.has("channel")) {
            sitting.get("channel").forEach(channel -> {
                formattedHearingPlatform
                    .append(channel.asText())
                    .append(", ");
            });
        } else if (session.has("sessionChannel")) {
            session.get("sessionChannel").forEach(channel -> {
                formattedHearingPlatform
                    .append(channel.asText())
                    .append(", ");
            });
        }

        ((ObjectNode)sitting).put("caseHearingChannel",
            Helpers.trimAnyCharacterFromStringEnd(formattedHearingPlatform.toString().trim()));
    }

    private static void formattedCourtRoomName(JsonNode courtRoom, JsonNode session,
                                        StringBuilder formattedJudiciary) {
        if (!formattedJudiciary.toString().trim().isEmpty()) {
            formattedJudiciary.insert(0, courtRoom.get("courtRoomName").asText() + ": ");
        } else {
            formattedJudiciary.append(courtRoom.get("courtRoomName").asText());
        }

        ((ObjectNode)session).put("formattedSessionCourtRoom",
                                  Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()));
    }

    private static String findAndManipulateJudiciary(JsonNode session) {
        AtomicReference<StringBuilder> formattedJudiciary = new AtomicReference<>(new StringBuilder());
        AtomicReference<Boolean> foundPresiding = new AtomicReference<>(false);

        if (session.has("judiciary")) {
            session.get("judiciary").forEach(judiciary -> {
                if (Helpers.findAndReturnNodeText(judiciary, "isPresiding")
                    .equals("true")) {
                    formattedJudiciary.set(new StringBuilder());
                    formattedJudiciary.get().append(Helpers.findAndReturnNodeText(judiciary, "johKnownAs"));
                    foundPresiding.set(true);
                } else if (!foundPresiding.get()) {
                    formattedJudiciary.get()
                        .append(Helpers.findAndReturnNodeText(judiciary, "johKnownAs"))
                        .append(", ");
                }
            });

            if (!Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString()).isEmpty()) {
                formattedJudiciary.get().insert(0, "Before: ");
            }
        }

        return Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString());
    }
}
