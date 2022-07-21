package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templateModels.sjpPressCase;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SjpPressListConverter implements Converter {

    Helpers helpers = new Helpers();

    public static final String INDIVIDUAL_DETAILS = "individualDetails";

    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        List<sjpPressCase> caseList = new ArrayList<>();
        Iterator<JsonNode> hearingNode =
            artefact.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (hearingNode.hasNext()) {
            JsonNode currentCase = hearingNode.next();
            sjpPressCase thisCase = new sjpPressCase();
            processRoles(thisCase, currentCase);
            processCaseUrns(thisCase, currentCase.get("case"));
            processOffences(thisCase, currentCase.get("offence"));
            caseList.add(thisCase);
        }

        String publishedDate = helpers.formatTimestampToBst(artefact.get("document").get("publicationDate").asText());
        context.setVariable("contentDate", helpers.formatTimestampToBst(metadata.get("contentDate")));
        context.setVariable("publishedDate", publishedDate);
        context.setVariable("jsonBody", artefact);
        context.setVariable("metaData", metadata);
        context.setVariable("cases", caseList);
        context.setVariable("artefact", artefact);
        return templateEngine.process("sjpPressList.html", context);
    }

    void processRoles(sjpPressCase currentCase, JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if ("accused".equals(currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT))) {
                JsonNode individualDetailsNode = currentParty.get(INDIVIDUAL_DETAILS);
                currentCase.setName(individualDetailsNode.get("individualForenames").asText() + " " +
                                        individualDetailsNode.get("individualSurname").asText());
                processAddress(currentCase, individualDetailsNode.get("address"));
                currentCase.setDateOfBirth(individualDetailsNode.get("dateOfBirth").asText());
                currentCase.setAge(individualDetailsNode.get("age").asText());
            } else {
                currentCase.setProsecutor(currentParty.get("organisationDetails").get("organisationName").asText());
            }
        }
    }

    void processCaseUrns(sjpPressCase currentCaseNode, JsonNode caseNode) {
        List<String> caseUrns = new ArrayList<>();
        for (final JsonNode currentCase : caseNode) {
            caseUrns.add(currentCase.get("caseUrn").asText());
        }
        currentCaseNode.setReference1(caseUrns.get(0));
        currentCaseNode.setReferenceRemainder(caseUrns.stream().skip(1).collect(Collectors.toList()));
    }

    void processAddress(sjpPressCase thisCase, JsonNode addressNode) {
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
        thisCase.setAddressRemainder(address.stream().skip(1).collect(Collectors.toList()));
    }


    void processOffences(sjpPressCase currentCase, JsonNode offencesNode) {
        List<Map<String, String>> listOfOffences = new ArrayList<>();
        Iterator<JsonNode> offences = offencesNode.elements();
        while (offences.hasNext()) {
            JsonNode thisOffence = offences.next();
            Map<String, String> thisOffenceMap = new HashMap<>();
            thisOffenceMap.put("offence", thisOffence.get("offenceTitle").asText());
            thisOffenceMap.put("reportingRestriction", processReportingRestrictionsjpPress(thisOffence));
            thisOffenceMap.put("wording", thisOffence.get("offenceWording").asText());
            listOfOffences.add(thisOffenceMap);
        }
        currentCase.setOffences(listOfOffences);
    }

    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "Active";
        } else {
            return "None";
        }
    }

}
