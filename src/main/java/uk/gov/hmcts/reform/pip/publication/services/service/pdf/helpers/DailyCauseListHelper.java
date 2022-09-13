package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;

import java.util.Map;

public final class DailyCauseListHelper {
    private DailyCauseListHelper() {
    }

    public static Context preprocessArtefactForThymeLeafConverter(JsonNode artefact, Map<String, String> metadata) {
        Context context = new Context();
        String publicationDate = artefact.get("document").get("publicationDate").asText();
        DataManipulation.formatCourtAddress(artefact);

        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               true, false));
        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable("locationName", metadata.get("locationName"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("venueAddress", DataManipulation.formatVenueAddress(artefact));
        context.setVariable("artefact", artefact);

        DataManipulation.manipulatedDailyListData(artefact);
        return context;
    }
}
