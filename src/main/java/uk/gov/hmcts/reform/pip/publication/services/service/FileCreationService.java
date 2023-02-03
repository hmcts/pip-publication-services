package uk.gov.hmcts.reform.pip.publication.services.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.ExcelGenerationService;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to create files to send in emails.
 */
@Slf4j
@Service
@SuppressWarnings("PMD.PreserveStackTrace")
public class FileCreationService {

    @Autowired
    private DataManagementService dataManagementService;

    @Autowired
    private AccountManagementService accountManagementService;

    @Autowired
    private SubscriptionManagementService subscriptionManagementService;

    @Autowired
    private ExcelGenerationService excelGenerationService;

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
            List<String[]> data = new ArrayList<>(Collections.singleton(HEADINGS));
            mediaApplicationList.forEach(application -> data.add(application.toCsvStringArray()));
            csvWriter.writeAll(data);
            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CsvCreationException(e.getMessage());
        }
    }

    /**
     * Calls out to data management, account management and subscription management services to get the MI data and
     * generates an Excel spreadsheet returned as a byte array.
     *
     * @return a byte array of the Excel spreadsheet.
     * @throws IOException if an error appears during Excel generation.
     */
    public byte[] generateMiReport() throws IOException {
        return excelGenerationService.generateMultiSheetWorkBook(extractMiData());
    }

    private Map<String, List<String[]>> extractMiData() {
        Map<String, List<String[]>> data = new ConcurrentHashMap<>();

        List<String[]> artefactData = formatData(dataManagementService.getMiData());
        if (!artefactData.isEmpty()) {
            data.put("Publications", artefactData);
        }

        List<String[]> userData = formatData(accountManagementService.getMiData());
        if (!userData.isEmpty()) {
            data.put("User accounts", userData);
        }

        List<String[]> allSubscriptionData = formatData(subscriptionManagementService.getAllMiData());
        if (!allSubscriptionData.isEmpty()) {
            data.put("All subscriptions", allSubscriptionData);
        }

        List<String[]> locationSubscriptionData = formatData(subscriptionManagementService.getLocationMiData());
        if (!locationSubscriptionData.isEmpty()) {
            data.put("Location subscriptions", locationSubscriptionData);
        }
        return data;
    }

    private List<String[]> formatData(String data) {
        return data.lines()
            .map(l -> l.split(","))
            .toList();
    }
}
