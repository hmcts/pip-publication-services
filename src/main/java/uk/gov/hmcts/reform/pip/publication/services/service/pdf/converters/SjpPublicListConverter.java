package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.util.Map;

public class SjpPublicListConverter implements Converter {

    @Override

    public String convert(JsonNode artefact, Map<String, String> metadata) {
        Context context = new Context();
        String date = artefact.get("document").get("publicationDate").asText();

        context.setVariable("date", date);
        context.setVariable("jsonBody", artefact);

        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sjpPublicList.html", context);
    }

}
