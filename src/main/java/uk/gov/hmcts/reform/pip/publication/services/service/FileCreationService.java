package uk.gov.hmcts.reform.pip.publication.services.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.PublicationMiData;
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
public class FileCreationService {

    private static final String[] HEADINGS = {"Full name", "Email", "Employer",
        "Request date", "Status", "Status date"};

    private final DataManagementService dataManagementService;
    private final AccountManagementService accountManagementService;
    private final ExcelGenerationService excelGenerationService;

    @Autowired
    public FileCreationService(DataManagementService dataManagementService,
                               AccountManagementService accountManagementService,
                               ExcelGenerationService excelGenerationService) {
        this.dataManagementService = dataManagementService;
        this.accountManagementService = accountManagementService;
        this.excelGenerationService = excelGenerationService;
    }

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
     * Calls out to data management and account management services to get the MI data and generates an Excel
     * spreadsheet returned as a byte array.
     *
     * @return a byte array of the Excel spreadsheet.
     * @throws IOException if an error appears during Excel generation.
     */
    public byte[] generateMiReport() throws IOException {
        return excelGenerationService.generateMultiSheetWorkBook(extractMiData());
    }

    Map<String, List<String[]>> extractMiData() {
        Map<String, List<String[]>> data = new ConcurrentHashMap<>();

        List<String[]> artefactData = new ArrayList<>();
        artefactData.add(PublicationMiData.generateReportHeaders());
        artefactData.addAll(dataManagementService.getMiData()
                                .stream().map(PublicationMiData::generateReportData).toList());
        data.put("Publications", artefactData);

        List<String[]> userData = new ArrayList<>();
        userData.add(AccountMiData.generateReportHeaders());
        userData.addAll(accountManagementService.getAccountMiData()
                            .stream().map(AccountMiData::generateReportData).toList());
        data.put("User accounts", userData);

        List<String[]> allSubscriptionData = new ArrayList<>();
        allSubscriptionData.add(AllSubscriptionMiData.generateReportHeaders());
        allSubscriptionData.addAll(accountManagementService.getAllSubscriptionMiData()
                                       .stream().map(AllSubscriptionMiData::generateReportData).toList());
        data.put("All subscriptions", allSubscriptionData);

        List<String[]> locationSubscriptionData = new ArrayList<>();
        locationSubscriptionData.add(LocationSubscriptionMiData.generateReportHeaders());
        locationSubscriptionData.addAll(accountManagementService.getLocationSubscriptionMiData()
                                            .stream().map(LocationSubscriptionMiData::generateReportData).toList());
        data.put("Location subscriptions", locationSubscriptionData);

        return data;
    }
}
