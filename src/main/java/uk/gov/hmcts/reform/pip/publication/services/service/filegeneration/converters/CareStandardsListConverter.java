package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.TribunalNationalListsManipulation;

import java.util.Map;

@Service
public class CareStandardsListConverter implements Converter {
    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata, Map<String, Object> languageResources) {
        Context context = TribunalNationalListsManipulation
            .preprocessArtefactForTribunalNationalListsThymeLeafConverter(artefact, metadata, languageResources);
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("careStandardsList.html", context);
    }
}
