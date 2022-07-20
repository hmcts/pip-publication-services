package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templateModels.sjpPressCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SjpPressListConverter implements Converter {
    public static final String INDIVIDUAL_DETAILS = "individualDetails";

    @Override
    public String convert(JsonNode artefact) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        String date = artefact.get("document").get("publicationDate").asText();
        List<sjpPressCase>  caseList= new ArrayList<>();
        Iterator<JsonNode> hearingNode =
            artefact.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
                "sittings").get(0).get("hearing").elements();
        while (hearingNode.hasNext()) {
            JsonNode currentCase = hearingNode.next();
            sjpPressCase thisCase = new sjpPressCase();
            processRoles(thisCase, currentCase);
//            thisCase.setReference(currentCase.get("case").get("caseUrn").asText());
            caseList.add(thisCase);
        }
        context.setVariable("date", date);
        context.setVariable("jsonBody", artefact);
        context.setVariable("cases", caseList);
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
                currentCase.setAddress(processAddress(individualDetailsNode.get("address")));
                currentCase.setDateOfBirth(individualDetailsNode.get("dateOfBirth").asText());
            } else {
                currentCase.setProsecutor(currentParty.get("organisationDetails").get("organisationName").asText());
            }
        }
    }

    List<String> processAddress(JsonNode addressNode) {
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
        return address;
    }


    sjpPressCase processOffences(sjpPressCase currentCase, JsonNode offencesNode) {
        List<Map<String, String>> listOfOffences = new ArrayList<>();
        Iterator<JsonNode> offences = offencesNode.elements();
        while (offences.hasNext()) {
            JsonNode thisOffence = offences.next();
            Map<String, String> thisOffenceMap = new HashMap<>();
            thisOffenceMap.put("offence", thisOffence.get("offenceTitle").asText());
            thisOffenceMap.put("reportingRestriction", processReportingRestrictionsjpPress(thisOffence));
            thisOffenceMap.put("wording", thisOffence.get("offenceTitle").asText());
            listOfOffences.add(thisOffenceMap);
        }
        currentCase.setOffences(listOfOffences);
        return currentCase;
    }

    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "Reporting Restriction";
        } else {
            return "No Reporting Restriction";
        }
    }

}
