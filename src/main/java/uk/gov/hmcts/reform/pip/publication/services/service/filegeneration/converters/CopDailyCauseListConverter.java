package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.util.Map;

@Service
public class CopDailyCauseListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) {
        Context context = new Context();
        String publicationDate = artefact.get("document").get("publicationDate").asText();

        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               Language.valueOf(metadata.get(
                                                                                   "language")),
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               Language.valueOf(metadata.get(
                                                                                   "language")),
                                                                               true, false));
        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable("locationName", metadata.get("locationName"));
        context.setVariable("locationDetails", artefact.get("locationDetails"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("artefact", artefact);
        context.setVariable("i18n", language);

        DataManipulation.manipulateCopListData(artefact, Language.valueOf(metadata.get("language")));

        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("copDailyCauseList.html", context);
    }
}
