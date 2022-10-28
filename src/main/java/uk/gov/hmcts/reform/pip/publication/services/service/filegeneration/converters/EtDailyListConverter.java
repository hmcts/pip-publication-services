package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.EtDailyListManipulation;

import java.util.Map;

@Service
public class EtDailyListConverter implements Converter {
    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata, Map<String, Object> languageResources) {
        Context context = new Context();
        Language language = Language.valueOf(metadata.get("language"));
        setPublicationDateTime(context, artefact.get("document").get("publicationDate").asText(), language);

        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable("locationName", metadata.get("locationName"));
        context.setVariable("region", metadata.get("region"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("i18n", languageResources);

        EtDailyListManipulation.processRawListData(artefact, language);
        context.setVariable("artefact", artefact);
        setVenue(context, artefact.get("venue"));

        return new ThymeleafConfiguration().templateEngine().process("etDailyList.html", context);
    }

    private void setPublicationDateTime(Context context, String publicationDate, Language language) {
        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               true, false));
    }

    private void setVenue(Context context, JsonNode venue) {
        context.setVariable("venueName", GeneralHelper.findAndReturnNodeText(venue,"venueName"));

        String venueEmail = "";
        String venueTelephone = "";
        if (venue.has("venueContact")) {
            venueEmail = GeneralHelper.findAndReturnNodeText(venue.get("venueContact"),"venueEmail");
            venueTelephone = GeneralHelper.findAndReturnNodeText(venue.get("venueContact"),"venueTelephone");
        }

        context.setVariable("venueEmail", venueEmail);
        context.setVariable("venueTelephone", venueTelephone);
    }

}
