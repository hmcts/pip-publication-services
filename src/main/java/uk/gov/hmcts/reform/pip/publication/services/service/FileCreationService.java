package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.Converter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.DateHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for functionality related to building PDF and xlsx files from JSON input. Thymeleaf templates are used to
 * build HTML documents which are then translated to PDF by the OpenHTMLtoPDF Library. Importantly, we're using the
 * second-highest possible level of PDF accessibility, which means that when developing new templates, we must listen
 * carefully to the warnings output by the compiler.
 */
@Slf4j
@Service
@SuppressWarnings("PMD.PreserveStackTrace")
public class FileCreationService {

    private static final String[] HEADINGS = {"Full name", "Email", "Employer",
        "Request date", "Status", "Status date"};

    @Autowired
    private DataManagementService dataManagementService;

    /**
     * Wrapper class for the entire json to pdf process.
     *
     * @param inputPayloadUuid UUID representing a particular artefact ID.
     * @return byteArray representing the generated PDF.
     * @throws IOException - uses file streams so needs this.
     */
    public String jsonToHtml(UUID inputPayloadUuid) throws IOException {
        String rawJson = dataManagementService.getArtefactJsonBlob(inputPayloadUuid);
        Artefact artefact = dataManagementService.getArtefact(inputPayloadUuid);
        Location location = dataManagementService.getLocation(artefact.getLocationId());

        JsonNode topLevelNode = new ObjectMapper().readTree(rawJson);
        Map<String, String> metadataMap = Map.of(
            "contentDate", DateHelper.formatLocalDateTimeToBst(artefact.getContentDate()),
            "provenance", artefact.getProvenance(),
            "locationName", location.getName(),
            "language", artefact.getLanguage().toString()
        );

        Converter converter = artefact.getListType().getConverter();

        return (converter == null)
            ? parseThymeleafTemplate(rawJson)
            : converter.convert(topLevelNode, metadataMap);
    }

    /**
     * Class which takes in JSON input and uses it to inform a given template. Consider this a placeholder until we
     * have specific style guides created.
     *
     * @param json - json string input representing a publication
     * @return formatted html string representing the input to the pdf reader
     */
    private String parseThymeleafTemplate(String json) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        context.setVariable("jsonBody", json);
        return templateEngine.process("testTemplate.html", context);
    }

    /**
     * Class which takes in an HTML file and generates an accessible PDF file (as a byteArray). This originally used
     * an Opentype font (.otf) but it reduced the file size to switch to a Truetype font (.ttf).
     *
     * @param html - string input representing a well-formed HTML file conforming to WCAG pdf accessibility guidance
     * @return a byte array representing the generated PDF.
     * @throws IOException - if errors appear during the process.
     */
    public byte[] generatePdfFromHtml(String html, boolean accessibility) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode()
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_A);

            File file = new File("/opt/app/gdsFont.ttf");
            if (file.exists()) {
                builder.useFont(file, "GDS Transport");
            } else {
                builder.useFont(new File(Thread.currentThread().getContextClassLoader()
                                             .getResource("gdsFont.ttf").getFile()), "GDS Transport");
            }
            if (accessibility) {
                builder.usePdfUaAccessbility(true);
            }

            builder.withHtmlContent(html, null)
                .toStream(baos)
                .run();
            return baos.toByteArray();
        }
    }

    /**
     * Takes in a payload id and generates an Excel spreadsheet returned as a byte array.
     *
     * @param payloadId The ID of the artefact to create a spreadsheet from.
     * @return A byte array of the Excel spreadsheet.
     * @throws IOException If an error appears during the process.
     */
    public byte[] generateExcelSpreadsheet(UUID payloadId) throws IOException {
        String rawJson = dataManagementService.getArtefactJsonBlob(payloadId);
        Artefact artefact = dataManagementService.getArtefact(payloadId);

        JsonNode topLevelNode = new ObjectMapper().readTree(rawJson);
        Converter converter = artefact.getListType().getConverter();

        return converter.convertToExcel(topLevelNode);
    }

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
            throw new CsvCreationException(e.getMessage());
        }
    }
}
