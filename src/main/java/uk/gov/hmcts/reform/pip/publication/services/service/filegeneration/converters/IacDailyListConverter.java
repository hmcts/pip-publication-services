package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.io.IOException;
import java.util.Map;

/**
 * Converter class for the IAC daily list to generate the PDF
 */
public class IacDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) throws IOException {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();

        Context context = new Context();
        context.setVariable("i18n", language);
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("artefact", artefact);
        context.setVariable("telephone", artefact.get("venue").get("venueContact").get("venueTelephone").asText());
        context.setVariable("email", artefact.get("venue").get("venueContact").get("venueEmail").asText());
        return templateEngine.process("iacDailyList.html", context);
    }

    private void extractCourtLists() {

    }

}
