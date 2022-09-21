package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class SscsDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata) throws IOException {
        Context context = new Context();
        context.setVariable("i18n", handleLanguages(metadata));
        context.setVariable("metadata", metadata);
        context.setVariable("telephone", GeneralHelper.safeGet("venue.venueContact.venueTelephone", highestLevelNode));
        context.setVariable("email", GeneralHelper.safeGet("venue.venueContact.venueEmail", highestLevelNode));
        context.setVariable("publishedDate", DateHelper.formatTimestampToBst(
            GeneralHelper.safeGet("document.publicationDate", highestLevelNode)));
        List<CourtHouse> listOfCourtHouses = new ArrayList<>();
        for (JsonNode courtHouse : highestLevelNode.get("courtLists")) {
            listOfCourtHouses.add(DataManipulation.courtHouseBuilder(courtHouse));
        }
        context.setVariable("courtList", listOfCourtHouses);
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sscsDailyList.html", context);
    }

    private Map<String, Object> handleLanguages(Map<String, String> metadata) throws IOException {
        String path;
        switch (metadata.get("language")) {
            case "ENGLISH": {
                path = "templates/languages/sscs-english.json";
                break;
            }
            case "WELSH":
                // todo: replace with welsh file or refactor to include it as an arg in converter interface.
                path = "templates/languages/sscs-english.json";

                break;
            case "BI_LINGUAL":
                // todo: replace with welsh file or refactor to include it as an arg in converter interface.
                path = "templates/languages/sscs-english.json";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        try (InputStream languageFile = Thread.currentThread()
            .getContextClassLoader().getResourceAsStream(path)) {
            return new ObjectMapper().readValue(
                Objects.requireNonNull(languageFile).readAllBytes(), new TypeReference<>() {
                });
        }
    }
}



