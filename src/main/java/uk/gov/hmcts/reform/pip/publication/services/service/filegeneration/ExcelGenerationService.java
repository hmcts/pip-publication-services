package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExcelGenerationService extends ExcelAbstractList {
    /**
     * Converts data to multi-sheet Excel spreadsheet.
     *
     * @param sheets the data containing the sheet name and the row information in the form of a list of string array.
     * @return the converted Excel spreadsheet as a byte array.
     * @throws IOException if an error appears during Excel generation.
     */
    public byte[] generateMultiSheetWorkBook(Map<String, List<String[]>> sheets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            AtomicInteger sheetIdx = new AtomicInteger();
            sheets.forEach((k, v) -> generateSheet(workbook, k, v, sheetIdx.getAndIncrement()));
            return convertToByteArray(workbook);
        }
    }

    private void generateSheet(Workbook workbook, String sheetName, List<String[]> data, int sheetNumber) {
        Sheet sheet = workbook.createSheet(sheetName);
        workbook.setSheetOrder(sheetName, sheetNumber);
        buildHeaderAndData(workbook, sheet, data);
        autoSizeSheet(sheet);
    }

    private void buildHeaderAndData(Workbook workbook, Sheet sheet, List<String[]> data) {
        AtomicInteger rowIdx = new AtomicInteger();
        Row headingRow = sheet.createRow(rowIdx.getAndIncrement());
        setRow(data.get(0), headingRow, createBoldStyle(workbook));

        data.stream()
            .skip(1)
            .forEach(r -> {
                Row dataRow = sheet.createRow(rowIdx.getAndIncrement());
                setRow(r, dataRow, null);
            });
    }

    private void setRow(String[] rowValue, Row row, CellStyle cellStyle) {
        AtomicInteger cellIdx = new AtomicInteger();
        Arrays.stream(rowValue)
            .forEach(cell -> setCellValue(row, cellIdx.getAndIncrement(), cell, cellStyle));
    }
}
