package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;
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

@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public class SjpPressListConverter implements Converter {

    Helpers helpers = new Helpers();

    public static final String INDIVIDUAL_DETAILS = "individualDetails";

    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata) {
        Context context = new Context();
        List<SjpPressList> caseList = new ArrayList<>();
        int count = 1;
        Iterator<JsonNode> hearingNode =
            artefact.get("courtLists").get(0).get("courtHouse").get("courtRoom").get(0).get("session").get(0).get(
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

        String publishedDate = helpers.formatTimestampToBst(
            artefact.get("document").get("publicationDate").asText(),
            false
        );
        context.setVariable("contentDate", helpers.formatTimestampToBst(
            metadata.get("contentDate"),
            true
        ));
        context.setVariable("publishedDate", publishedDate);
        context.setVariable("jsonBody", artefact);
        context.setVariable("metaData", metadata);
        context.setVariable("cases", caseList);
        context.setVariable("artefact", artefact);
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sjpPressList.html", context);
    }

    void processRoles(SjpPressList currentCase, JsonNode party) {
        Iterator<JsonNode> partyNode = party.get("party").elements();
        while (partyNode.hasNext()) {
            JsonNode currentParty = partyNode.next();
            if ("accused".equals(currentParty.get("partyRole").asText().toLowerCase(Locale.ROOT))) {
                JsonNode individualDetailsNode = currentParty.get(INDIVIDUAL_DETAILS);
                currentCase.setName(individualDetailsNode.get("individualForenames").asText() + " "
                                        + individualDetailsNode.get("individualSurname").asText());
                processAddress(currentCase, individualDetailsNode.get("address"));
                currentCase.setDateOfBirth(individualDetailsNode.get("dateOfBirth").asText());
                currentCase.setAge(individualDetailsNode.get("age").asText());
            } else {
                currentCase.setProsecutor(currentParty.get("organisationDetails").get("organisationName").asText());
            }
        }
    }

    void processCaseUrns(SjpPressList currentCaseNode, JsonNode caseNode) {
        List<String> caseUrns = new ArrayList<>();
        for (final JsonNode currentCase : caseNode) {
            caseUrns.add(currentCase.get("caseUrn").asText());
        }
        currentCaseNode.setReference1(caseUrns.get(0));
        currentCaseNode.setReferenceRemainder(caseUrns.stream().skip(1).collect(Collectors.toList()));
    }

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

    private String processReportingRestrictionsjpPress(JsonNode node) {
        boolean restriction = node.get("reportingRestriction").asBoolean();
        if (restriction) {
            return "Active";
        } else {
            return "None";
        }
    }

}
