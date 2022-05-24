package uk.gov.hmcts.reform.pip.publication.services.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Service to assist with transforming data in to CSVs for gov.uk notify
 */
@Service
@Slf4j
public class CsvCreationService {
    private static final String[] HEADINGS = {"Full name", "Email", "Employer",
        "Request date", "Status", "Status date"};

    /**
     * Creates a byte array csv from a list of media applications.
     *
     * @param mediaApplicationList The list of media applications to form the csv
     * @return A byte array of the csv file
     */
    public byte[] createMediaApplicationReportingCsv(List<MediaApplication> mediaApplicationList) {
        try (StringWriter sw = new StringWriter(); CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(HEADINGS);
            mediaApplicationList.forEach(application ->
                csvWriter.writeNext(application.toCsvStringArray()));

            return sw.toString().getBytes("UTF-8");
        } catch (IOException e) {
            log.error("Error occurred %s when trying to create a csv with list %s",
                      e.getMessage(), mediaApplicationList);
            throw new CsvCreationException(e.getMessage());
        }
    }
}
