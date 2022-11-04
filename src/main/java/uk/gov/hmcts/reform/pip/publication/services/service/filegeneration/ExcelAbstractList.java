package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Abstract class containing reusable methods for Excel spreadsheet generation.
 */
@SuppressWarnings({"PMD.AbstractClassWithoutAbstractMethod"})
public abstract class ExcelAbstractList {

    /**
     * Set a given cells value with a given value.
     */
    protected void setCellValue(Row row, int cellNumber, String value) {
        setCellValue(row, cellNumber, value, null);
    }

    /**
     * Set a given cells value with a given value and style.
     */
    protected void setCellValue(Row row, int cellNumber, String value, CellStyle style) {
        Cell cell = row.createCell(cellNumber);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    /**
     * Create a bold style for a cell.
     */
    protected CellStyle createBoldStyle(Workbook wb) {
        CellStyle cellStyle =  wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        cellStyle.setFont(font);
        return cellStyle;
    }

    /**
     * Autosize the spreadsheet.
     */
    protected void autoSizeSheet(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row row = sheet.getRow(sheet.getFirstRowNum());
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                int columnIndex = cell.getColumnIndex();
                sheet.autoSizeColumn(columnIndex);
            }
        }
    }

    /**
     * Convert the spreadsheet to a byte array to send with notify.
     */
    protected byte[] convertToByteArray(Workbook workbook) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        return baos.toByteArray();
    }
}
