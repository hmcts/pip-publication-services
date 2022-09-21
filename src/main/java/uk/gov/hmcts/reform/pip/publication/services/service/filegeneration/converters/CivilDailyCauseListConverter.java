package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DailyCauseListHelper.preprocessArtefactForThymeLeafConverter;

public class CivilDailyCauseListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact, Map<String, String> artefactValues) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("civilDailyCauseList.html",
                                      preprocessArtefactForThymeLeafConverter(artefact, artefactValues));
    }
}
