package uk.gov.hmcts.reform.pip.publication.services.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;

// Add needed annotations and comments
@Service
@Slf4j
public class CsvCreationService {


    // LOTS OF CLEAN UP TO DO ON THIS FILE
    // ADD JAVA DOC
    // ADD IN THE ACTUAL SORTING OF THE REAL DATA TOO
    public byte[] createMediaApplicationReportingCsv() throws IOException {
        String[] headings = {"test", "test2", "test3", "test4"};

        try (StringWriter sw = new StringWriter(); CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(headings);

            byte[] csv = sw.toString().getBytes("UTF-8");

            return csv;

            // Need to catch Exception in here and throw it try/catch block
        }
    }
}
