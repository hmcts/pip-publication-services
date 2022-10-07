package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.SjpManipulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class SjpPublicListConverter extends ExcelAbstractList implements Converter {
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
        context.setVariable("cases", processRawListData(artefact));

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

            processRawListData(artefact).forEach(entry -> {
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

    private List<SjpPublicList> processRawListData(JsonNode data) {
        List<SjpPublicList> sjpCases = new ArrayList<>();

        data.get("courtLists").forEach(courtList -> {
            courtList.get("courtHouse").get("courtRoom").forEach(courtRoom -> {
                courtRoom.get("session").forEach(session -> {
                    session.get("sittings").forEach(sitting -> {
                        sitting.get("hearing").forEach(hearing -> {
                            Optional<SjpPublicList> sjpCase = SjpManipulation.constructSjpCase(hearing);
                            if (sjpCase.isPresent()) {
                                sjpCases.add(sjpCase.get());
                            }
                        });
                    });
                });
            });
        });
        return sjpCases;
    }
}
