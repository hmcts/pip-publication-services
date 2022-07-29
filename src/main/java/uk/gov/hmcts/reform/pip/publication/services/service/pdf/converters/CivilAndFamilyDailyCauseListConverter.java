package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.util.Map;

@Service
public class CivilAndFamilyDailyCauseListConverter implements Converter {
    @Override
    public String convert(JsonNode artefact, Map<String, String> artefactValues) {
        Context context = new Context();
        String publicationDate = artefact.get("document").get("publicationDate").asText();
        DataManipulation.formatCourtAddress(artefact);

        context.setVariable("publicationDate", Helpers.formatTimeStampToBst(publicationDate,
                                                                            false, false));
        context.setVariable("publicationTime", Helpers.formatTimeStampToBst(publicationDate,
                                                                            true, false));
        context.setVariable("contentDate", artefactValues.get("contentDate"));
        context.setVariable("locationName", artefactValues.get("locationName"));
        context.setVariable("provenance", artefactValues.get("provenance"));
        context.setVariable("venueAddress", DataManipulation.formatVenueAddress(artefact));
        context.setVariable("artefact", artefact);

        DataManipulation.manipulatedDailyListData(artefact);

        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("civilAndFamilyDailyCauseList.html", context);
    }
}
