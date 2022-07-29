package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPressList;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter class for SJP Press Lists - builds a nice pdf from input json and an html template (found in
 * resources/mocks). Uses Thymeleaf to take in variables from model and build appropriately. Final output string is
 * passed in to PDF Creation Service.
 */
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
@Service
public class SjpPressListConverter implements Converter {

    public static final String INDIVIDUAL_DETAILS = "individualDetails";

    /**
     * parent method for the process.
     * @param jsonBody - JsonNode representing the data within our jsonBody.
     * @param metadata - immutable map containing relevant data from request headers (i.e. not within json body) used
     *                to inform the template
     * @return - html string of final output
     */
    @Override
    public String convert(JsonNode jsonBody, Map<String, String> metadata) {
        Context context = new Context();
        List<SjpPressList> caseList = new ArrayList<>();
        int count = 1;
        Iterator<JsonNode> hearingNode =
            jsonBody.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (hearingNode.hasNext()) {
            JsonNode currentCase = hearingNode.next();
            SjpPressList thisCase = new SjpPressList();
            processRoles(thisCase, currentCase);
            processCaseUrns(thisCase, currentCase.get("case"));
            processOffences(thisCase, currentCase.get("offence"));
            thisCase.setCaseCounter(count);
            caseList.add(thisCase);
            count += 1;
        }

        String publishedDate = Helpers.formatTimestampToBstForSjp(
            jsonBody.get("document").get("publicationDate").asText()
        );
        context.setVariable("contentDate",
            metadata.get("contentDate")
        );
        context.setVariable("publishedDate", publishedDate);
        context.setVariable("jsonBody", jsonBody);
        context.setVariable("metaData", metadata);
        context.setVariable("cases", caseList);
        context.setVariable("artefact", jsonBody);
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sjpPressList.html", context);
    }

    /**
     * method for handling roles - sorts out accused and prosecutor roles and grabs relevant data from the json body.
     * @param thisCase - case model which is updated by the method.
     * @param party - node to be parsed.
     */
    void processRoles(SjpPressList thisCase, JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if ("accused".equals(currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT))) {
                JsonNode individualDetailsNode = currentParty.get(INDIVIDUAL_DETAILS);
                thisCase.setName(individualDetailsNode.get("individualForenames").asText() + " "
                                        + individualDetailsNode.get("individualSurname").asText());
                processAddress(thisCase, individualDetailsNode.get("address"));
                thisCase.setDateOfBirth(individualDetailsNode.get("dateOfBirth").asText());
                thisCase.setAge(individualDetailsNode.get("age").asText());
            } else {
                thisCase.setProsecutor(currentParty.get("organisationDetails").get("organisationName").asText());
            }
        }
    }

    /**
     * case URN processing method. Takes in a case and a case node and grabs all case urns. It is worth mentioning
     * that currently the case Urn field cannot safely be linked to offences where there are more
     * than one as they are on a different level of the json hierarchy.
     * @param thisCase - model representing case data.
     * @param caseNode - json node containing cases on given case.
     */
    void processCaseUrns(SjpPressList thisCase, JsonNode caseNode) {
        List<String> caseUrns = new ArrayList<>();
        for (final JsonNode currentCase : caseNode) {
            caseUrns.add(currentCase.get("caseUrn").asText());
        }
        thisCase.setReference1(caseUrns.get(0));
        thisCase.setReferenceRemainder(caseUrns.stream().skip(1).collect(Collectors.toList()));
    }

    /**
     * Handling address lines for the model.
     * @param thisCase - our case model.
     * @param addressNode - our node containing address data.
     */
    void processAddress(SjpPressList thisCase, JsonNode addressNode) {
        List<String> address = new ArrayList<>();
        JsonNode lineArray = addressNode.get("line");
        if (lineArray.isArray()) {
            for (final JsonNode addressLine : lineArray) {
                address.add(addressLine.asText());
            }
        }
        address.add(addressNode.get("town").asText());
        address.add(addressNode.get("county").asText());
        address.add(addressNode.get("postCode").asText());
        thisCase.setAddressLine1(address.get(0));
        thisCase.setAddressRemainder(address.stream()
                                         .skip(1)
                                         .filter(e -> !e.isEmpty())
                                         .collect(Collectors.toList()));
    }

    /**
     * Method which populates the offence list in our case model.
     * @param currentCase - case model.
     * @param offencesNode - node containing our offence data.
     */
    void processOffences(SjpPressList currentCase, JsonNode offencesNode) {
        List<Map<String, String>> listOfOffences = new ArrayList<>();
        Iterator<JsonNode> offences = offencesNode.elements();
        while (offences.hasNext()) {
            JsonNode thisOffence = offences.next();
            Map<String, String> thisOffenceMap = Map.of("offence",
                                                        thisOffence.get("offenceTitle").asText(),
                                                        "reportingRestriction",
                                                        processReportingRestrictionsjpPress(thisOffence),
                                                        "wording",
                                                        thisOffence.get("offenceWording").asText());
            listOfOffences.add(thisOffenceMap);
        }
        currentCase.setOffences(listOfOffences);
    }

    /**
     * Method to apply reporting restriction text to our model.
     * @param node - json node representing an offence.
     * @return a String containing the relevant text based on reporting restriction.
     */
    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "Active";
        } else {
            return "None";
        }
    }

}
