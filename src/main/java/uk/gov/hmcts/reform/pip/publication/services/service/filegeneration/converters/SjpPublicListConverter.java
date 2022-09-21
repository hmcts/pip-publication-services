package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPublicList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelAbstractList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SjpPublicListConverter extends ExcelAbstractList implements Converter {
    private static final String ACCUSED = "ACCUSED";
    private static final String PROSECUTOR = "PROSECUTOR";

    /**
     * Convert SJP public cases into HMTL file for PDF generation.
     *
     * @param artefact Tree object model for artefact
     * @param metadata Artefact metadata
     * @return the HTML representation of the SJP public cases
     */
    @Override
    public String convert(JsonNode artefact, Map<String, String> metadata) {
        Context context = new Context();
        String publicationDate = DateHelper.formatTimeStampToBst(
            artefact.get("document").get("publicationDate").textValue(), false, true
        );
        context.setVariable("publicationDate", publicationDate);
        context.setVariable("contentDate", metadata.get("contentDate"));

        List<SjpPublicList> cases = constructCases(
            artefact.get("courtLists").get(0)
                .get("courtHouse")
                .get("courtRoom")
                .get(0).get("session").get(0)
                .get("sittings")
        );
        context.setVariable("cases", cases);

        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        return templateEngine.process("sjpPublicList.html", context);
    }

    /**
     * Create SJP public list Excel spreadsheet from list data.
     *
     * @param artefact Tree object model for artefact.
     * @return The converted Excel spreadsheet as a byte array.
     */
    @Override
    public byte[] convertToExcel(JsonNode artefact) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("SJP Public List");
            CellStyle boldStyle = createBoldStyle(workbook);
            AtomicInteger rowIdx = new AtomicInteger();
            Row headingRow = sheet.createRow(rowIdx.getAndIncrement());
            setCellValue(headingRow, 0, "Name", boldStyle);
            setCellValue(headingRow, 1, "Postcode", boldStyle);
            setCellValue(headingRow, 2, "Offence", boldStyle);
            setCellValue(headingRow, 3, "Prosecutor", boldStyle);
            
            constructCases(
                artefact.get("courtLists").get(0)
                    .get("courtHouse")
                    .get("courtRoom")
                    .get(0).get("session").get(0)
                    .get("sittings")
            ).forEach(entry -> {
                Row dataRow = sheet.createRow(rowIdx.getAndIncrement());
                setCellValue(dataRow, 0, entry.getName());
                setCellValue(dataRow, 1, entry.getPostcode());
                setCellValue(dataRow, 2, entry.getOffence());
                setCellValue(dataRow, 3, entry.getProsecutor());
            });
            autoSizeSheet(sheet);

            return convertToByteArray(workbook);
        }
    }

    private List<SjpPublicList> constructCases(JsonNode sittingsNode) {
        List<SjpPublicList> sjpPublicLists = new ArrayList<>();
        sittingsNode.forEach(sitting -> {
            JsonNode hearingNode = sitting.get("hearing").get(0);
            Triple<String, String, String> parties = getCaseParties(hearingNode.get("party"));
            String offence = hearingNode.get("offence").get(0).get("offenceTitle").textValue();

            if (StringUtils.isNotBlank(parties.getLeft())
                && StringUtils.isNotBlank(parties.getMiddle())
                && StringUtils.isNotBlank(parties.getRight())
                && StringUtils.isNotBlank(offence)) {
                sjpPublicLists.add(
                    new SjpPublicList(parties.getLeft(), parties.getMiddle(), offence, parties.getRight())
                );
            }
        });
        return sjpPublicLists;
    }

    private Triple<String, String, String> getCaseParties(JsonNode partiesNode) {
        String name = null;
        String postcode = null;
        String prosecutor = null;

        for (JsonNode party : partiesNode) {
            String role = party.get("partyRole").textValue();
            if (ACCUSED.equals(role)) {
                JsonNode individual = party.get("individualDetails");
                name = buildNameField(individual);
                postcode = individual.get("address").get("postCode").textValue();
            } else if (PROSECUTOR.equals(role)) {
                prosecutor = party.get("organisationDetails").get("organisationName").textValue();
            }
        }
        return Triple.of(name, postcode, prosecutor);
    }

    private String buildNameField(JsonNode individual) {
        return individual.get("individualForenames").textValue()
            + " "
            + individual.get("individualSurname").textValue();
    }
}
