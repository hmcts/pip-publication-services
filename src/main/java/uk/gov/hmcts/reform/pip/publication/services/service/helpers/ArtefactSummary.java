package uk.gov.hmcts.reform.pip.publication.services.service.helpers;

import com.fasterxml.jackson.databind.JsonNode;

public final class ArtefactSummary {

    private ArtefactSummary() {
        throw new UnsupportedOperationException();
    }

    public static String processDailyCauseList(JsonNode node) {
        StringBuilder output = new StringBuilder();
        node.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            hearing.get("case").forEach(hearingCase -> {
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Name of Party(ies) - ");
                                output.append(Helpers.findAndReturnNodeText(hearingCase, "caseName"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Case ID - ");
                                output.append(Helpers.findAndReturnNodeText(hearingCase, "caseNumber"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Hearing Type - ");
                                output.append(Helpers.findAndReturnNodeText(hearing, "hearingType"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Location - ");
                                output.append(Helpers.findAndReturnNodeText(sitting, "caseHearingChannel"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Duration - ");
                                output.append(Helpers.findAndReturnNodeText(sitting, "formattedDuration"));
                                Helpers.appendToStringBuilder(output, "\n");
                                Helpers.appendToStringBuilder(output, "Judge - ");
                                output.append(Helpers.findAndReturnNodeText(session, "formattedSessionCourtRoom"));
                            });
                        });
                    });
                });
            });
        });

        return output.toString();
    }
}
