package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata, Map<String, Object> language)
        throws IOException {
        Context context = new Context();
        context.setVariable("i18n", language);
        context.setVariable("metadata", metadata);
        context.setVariable("telephone", GeneralHelper.safeGet("venue.venueContact.venueTelephone", highestLevelNode));
        context.setVariable("email", GeneralHelper.safeGet("venue.venueContact.venueEmail", highestLevelNode));
        context.setVariable("publishedDate", DateHelper.formatTimestampToBst(
            GeneralHelper.safeGet("document.publicationDate", highestLevelNode),
                Language.valueOf(metadata.get("language"))));

        List<CourtHouse> listOfCourtHouses = new ArrayList<>();
        for (JsonNode courtHouse : highestLevelNode.get("courtLists")) {
            listOfCourtHouses.add(DataManipulation.courtHouseBuilder(courtHouse));
        }
        context.setVariable("courtList", listOfCourtHouses);
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sscsDailyList.html", context);
    }
}



