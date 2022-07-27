package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.util.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD")
public final class DataManipulation {
    private static final String POSTCODE = "postCode";
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

            if (courtList.get("courtHouse").has("courtHouseAddress")) {
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
                if (courtHouseAddress.has(POSTCODE)) {
                    formattedCourtAddress
                        .append(courtHouseAddress.get(POSTCODE).asText())
                        .append('|');
                }
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

        if (hearing.has("party")) {
            hearing.get("party").forEach(party -> {
                if (!Helpers.findAndReturnNodeText(party, "partyRole").isEmpty()) {
                    switch (PartyRoleMapper.convertPartyRole(party.get("partyRole").asText())) {
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
                            break;
                    }
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
        }
        return "";
    }

    private static void calculateDuration(JsonNode sitting) {
        ZonedDateTime sittingStart = Helpers.convertStringToUtc(sitting.get("sittingStart").asText());
        ZonedDateTime sittingEnd = Helpers.convertStringToUtc(sitting.get("sittingEnd").asText());

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
