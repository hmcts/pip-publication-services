package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DataManipulation {
    private static final int MINUTES_PER_HOUR = 60;

    private DataManipulation() {
        throw new UnsupportedOperationException();
    }

    public static void manipulateCaseInformation(JsonNode hearingCase) {
        if (!Helpers.findAndReturnNodeText(hearingCase, "caseSequenceIndicator").isEmpty()) {
            ((ObjectNode)hearingCase).put("caseIndicator",
                                          "[" + hearingCase.get("caseSequenceIndicator").asText() + "]");
        }
    }

    public static void manipulateCopListData(JsonNode artefact) {
        formatRegionName(artefact);
        formatRegionalJoh(artefact);

        artefact.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    ((ObjectNode)session).put("formattedSessionJoh",
                                              DataManipulation.findAndManipulateJudiciary(session));
                    session.get("sittings").forEach(sitting -> {
                        DataManipulation.calculateDuration(sitting);
                        DataManipulation.findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(DataManipulation::manipulateCaseInformation);
                        });
                    });
                });
            });
        });
    }

    public static void calculateDuration(JsonNode sitting) {
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

    public static void findAndConcatenateHearingPlatform(JsonNode sitting, JsonNode session) {
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

    public static String findAndManipulateJudiciary(JsonNode session) {
        StringBuilder formattedJudiciary = new StringBuilder();

        try {
            session.get("judiciary").forEach(judiciary -> {
                if (formattedJudiciary.length() != 0) {
                    formattedJudiciary.append(", ");
                }

                formattedJudiciary.append(Helpers.findAndReturnNodeText(judiciary, "johTitle"));
                formattedJudiciary.append(" ");
                formattedJudiciary.append(Helpers.findAndReturnNodeText(judiciary, "johNameSurname"));
            });

        } catch (Exception ignored) {}

        return Helpers.trimAnyCharacterFromStringEnd(formattedJudiciary.toString());
    }

    public static void formatRegionName(JsonNode artefact) {
        try {
            ((ObjectNode) artefact).put("regionName", artefact.get("locationDetails").get("region").get("name").asText());
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

                formattedJoh.append(Helpers.findAndReturnNodeText(joh, "johKnownAs"));
                formattedJoh.append(" ");
                formattedJoh.append(Helpers.findAndReturnNodeText(joh, "johNameSurname"));
            });

            ((ObjectNode) artefact).put("regionalJoh", formattedJoh.toString());
        } catch (Exception e) {
            ((ObjectNode) artefact).put("regionalJoh", "");
        }
    }

}
