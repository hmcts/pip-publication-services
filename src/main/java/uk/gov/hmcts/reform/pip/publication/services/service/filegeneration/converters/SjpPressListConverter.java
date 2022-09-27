package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPressList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelAbstractList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.io.IOException;
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
@Service
public class SjpPressListConverter extends ExcelAbstractList implements Converter {

    public static final String INDIVIDUAL_DETAILS = "individualDetails";

    /**
     * parent method for the process.
     *
     * @param jsonBody - JsonNode representing the data within our jsonBody.
     * @param metadata - immutable map containing relevant data from request headers (i.e. not within json body) used
     *                 to inform the template
     * @return - html string of final output
     */
    @Override
    public String convert(JsonNode jsonBody, Map<String, String> metadata) {
        Context context = new Context();
        List<SjpPressList> caseList = processRawJson(jsonBody);

        String publishedDate = DateHelper.formatTimeStampToBst(
            jsonBody.get("document").get("publicationDate").asText(), false, true
        );
        context.setVariable(
            "contentDate",
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
     * Create SJP press list Excel spreadsheet from list data.
     *
     * @param artefact Tree object model for artefact.
     * @return The converted Excel spreadsheet as a byte array.
     */
    @Override
    public byte[] convertToExcel(JsonNode artefact) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            final List<SjpPressList> cases = processRawJson(artefact);

            Sheet sheet = workbook.createSheet("SJP Press List");
            CellStyle boldStyle = createBoldStyle(workbook);

            int rowIdx = 0;
            Row headingRow = sheet.createRow(rowIdx++);
            setCellValue(headingRow, 0, "Address", boldStyle);
            setCellValue(headingRow, 1, "Case URN", boldStyle);
            setCellValue(headingRow, 2, "Date of Birth", boldStyle);
            setCellValue(headingRow, 3, "Defendant Name", boldStyle);

            // Write out column headings for the max number of offences a defendant may have
            Integer maxOffences =
                cases.stream().map(SjpPressList::getNumberOfOffences).reduce(Integer::max).orElse(0);
            int offenceHeadingsIdx = 4;

            for (int i = 1; i <= maxOffences; i++) {
                setCellValue(headingRow, offenceHeadingsIdx,
                             String.format("Offence %o Press Restriction Requested", i),boldStyle);
                setCellValue(headingRow, ++offenceHeadingsIdx,
                             String.format("Offence %o Title", i), boldStyle);
                setCellValue(headingRow, ++offenceHeadingsIdx,
                             String.format("Offence %o Wording", i), boldStyle);
                offenceHeadingsIdx++;
            }
            setCellValue(headingRow, offenceHeadingsIdx, "Prosecutor Name", boldStyle);

            // Write out the data to the sheet
            for (SjpPressList entry : cases) {
                Row dataRow = sheet.createRow(rowIdx++);
                setCellValue(dataRow, 0, concatenateStrings(entry.getAddressLine1(),
                                                            String.join(" ", entry.getAddressRemainder())));
                setCellValue(dataRow, 1, concatenateStrings(entry.getReference1(),
                                                            String.join(" ", entry.getReferenceRemainder())));
                setCellValue(dataRow, 2, String.format("%s (%s)", entry.getDateOfBirth(), entry.getAge()));
                setCellValue(dataRow, 3, entry.getName());

                int offenceColumnIdx = 4;

                for (Map<String, String> offence : entry.getOffences()) {
                    setCellValue(dataRow, offenceColumnIdx, offence.get("reportingRestriction"));
                    setCellValue(dataRow, ++offenceColumnIdx, offence.get("offence"));
                    setCellValue(dataRow, ++offenceColumnIdx, offence.get("wording"));
                    ++offenceColumnIdx;
                }
                setCellValue(dataRow, offenceHeadingsIdx, entry.getProsecutor());
            }
            autoSizeSheet(sheet);

            return convertToByteArray(workbook);
        }
    }

    /**
     * Process the provided json body into a list of SjpPressList.
     *
     * @param jsonBody The raw json to process.
     * @return A list of SjpPressList.
     */
    List<SjpPressList> processRawJson(JsonNode jsonBody) {
        List<SjpPressList> caseList = new ArrayList<>();

        jsonBody.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            SjpPressList thisCase = new SjpPressList();
                            processRoles(thisCase, hearing);
                            processCaseUrns(thisCase, hearing.get("case"));
                            processOffences(thisCase, hearing.get("offence"));
                            thisCase.setNumberOfOffences(thisCase.getOffences().size());
                            caseList.add(thisCase);
                        });
                    });
                });
            });
        });

        return caseList;
    }

    /**
     * method for handling roles - sorts out accused and prosecutor roles and grabs relevant data from the json body.
     *
     * @param thisCase - case model which is updated by the method.
     * @param hearing    - node to be parsed.
     */
    void processRoles(SjpPressList thisCase, JsonNode hearing) {
        Iterator<JsonNode> partyNode = hearing.get("party").elements();
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
     *
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
     *
     * @param thisCase    - our case model.
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
     *
     * @param currentCase  - case model.
     * @param offencesNode - node containing our offence data.
     */
    void processOffences(SjpPressList currentCase, JsonNode offencesNode) {
        List<Map<String, String>> listOfOffences = new ArrayList<>();
        Iterator<JsonNode> offences = offencesNode.elements();
        while (offences.hasNext()) {
            JsonNode thisOffence = offences.next();
            Map<String, String> thisOffenceMap = Map.of(
                "offence", thisOffence.get("offenceTitle").asText(),
                "reportingRestriction", processReportingRestrictionsjpPress(thisOffence),
                "wording", thisOffence.get("offenceWording").asText()
            );
            listOfOffences.add(thisOffenceMap);
        }
        currentCase.setOffences(listOfOffences);
    }

    /**
     * Method to apply reporting restriction text to our model.
     *
     * @param node - json node representing an offence.
     * @return a String containing the relevant text based on reporting restriction.
     */
    private String processReportingRestrictionsjpPress(JsonNode node) {
        return node.get("reportingRestriction").asBoolean() ? "Active" : "None";
    }

    private String concatenateStrings(String... groupOfStrings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String stringToAdd : groupOfStrings) {
            stringBuilder.append(stringToAdd).append(' ');
        }
        return stringBuilder.toString();
    }
}
