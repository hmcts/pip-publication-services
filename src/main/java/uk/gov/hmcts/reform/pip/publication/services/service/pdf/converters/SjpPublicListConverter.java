package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.util.HashMap;
import java.util.Map;

public class SjpPublicListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact,  Map<String, String> metadata) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        String date = artefact.get("document").get("publicationDate").asText();

        context.setVariable("date", date);
        context.setVariable("jsonBody", artefact);

        Map<String, String> cases = new HashMap<>();
        cases.put("Josh", "hello");
        cases.put("Junaid", "hi");
        cases.put("Danny", "你好");
        cases.put("Kian", "ahoy!");
        cases.put("Chris", "hola");
        cases.put("Nigel", "bonjour");
        context.setVariable("cases", cases);
        return templateEngine.process("sjpPublicList.html", context);
    }

}
