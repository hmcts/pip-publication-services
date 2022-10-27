package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DataManipulation;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.GeneralHelper;

import java.io.IOException;
import java.util.Map;

/**
 * Converter class for the IAC daily list to generate the PDF.
 */
public class IacDailyListConverter implements Converter {

    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata,
                          Map<String, Object> language) throws IOException {
        Context context = new Context();

        calculateListData(artefact, Language.valueOf(metadata.get("language")));

        context.setVariable("i18n", language);
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("artefact", artefact);
        context.setVariable("contentDate", metadata.get("contentDate"));

        context.setVariable("locationName", metadata.get("locationName"));
        String publicationDate = artefact.get("document").get("publicationDate").asText();
        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               Language.valueOf(metadata.get(
                                                                                   "language")),
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate,
                                                                               Language.valueOf(metadata.get(
                                                                                   "language")),
                                                                               true, false));

        context.setVariable("telephone", artefact.get("venue").get("venueContact").get("venueTelephone").asText());
        context.setVariable("email", artefact.get("venue").get("venueContact").get("venueEmail").asText());

        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("iacDailyList.html", context);
    }

    /**
     * This method calculates the list data for the artefact.
     * @param artefact List data to calculate.
     * @param language The language for the list type.
     */
    private void calculateListData(JsonNode artefact, Language language) {
        artefact.get("courtLists").forEach(courtList -> {

            ((ObjectNode) courtList).put(
                "isBailList",
                "bail list".equalsIgnoreCase(courtList.get("courtListName")
                                                 .asText())
            );

            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {

                    String formattedJoh = DataManipulation.findAndManipulateJudiciaryForCop(session);
                    ((ObjectNode) session).put("formattedJudiciary", formattedJoh);

                    session.get("sittings").forEach(sitting -> {
                        String sittingStart = DateHelper.timeStampToBstTimeWithFormat(
                            sitting.get("sittingStart").asText(), "h:mma");

                        ((ObjectNode) sitting).put("formattedStart", sittingStart);

                        DataManipulation.findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get("hearing").forEach(hearing -> {
                            DataManipulation.findAndManipulatePartyInformation(hearing, language);
                            hearing.get("case").forEach(this::formatLinkedCases);
                        });
                    });
                });
            });

        });
    }

    /**
     * This method formats the linked cases.
     * @param caseInfo The case info that contains the linked cases.
     */
    private void formatLinkedCases(JsonNode caseInfo) {
        StringBuilder formattedLinked = new StringBuilder();

        if (caseInfo.has("caseLinked")) {
            caseInfo.get("caseLinked").forEach(linkedCase -> {

                if (formattedLinked.length() != 0) {
                    formattedLinked.append(", ");
                }
                formattedLinked.append(GeneralHelper.findAndReturnNodeText(linkedCase, "caseId"));
            });
        }

        ((ObjectNode) caseInfo).put("formattedLinkedCases", formattedLinked.toString());
    }

}
