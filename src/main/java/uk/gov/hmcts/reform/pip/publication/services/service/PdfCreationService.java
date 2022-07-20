package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.CivilDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.FamilyDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.SjpPressListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.SjpPublicListConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for functionality related to building PDF files from JSON input. Thymeleaf templates are used to build
 * HTML documents which are then translated to PDF by the OpenHTMLtoPDF Library. Importantly, we're using the highest
 * possible level of PDF accessibility, which means that when developing new templates, we must listen carefully to the
 * warnings output by the compiler.
 */

@Slf4j
@Service
public class PdfCreationService {

    @Autowired
    private DataManagementService dataManagementService;


    /**
     * Wrapper class for the entire json to pdf process.
     * @param inputPayloadUuid UUID representing a particular artefact ID.
     * @return byteArray representing the generated PDF.
     * @throws IOException - uses file streams so needs this.
     */
    public byte[] jsonToPdf(UUID inputPayloadUuid) throws IOException {
        String rawJson = dataManagementService.getArtefactJsonBlob(inputPayloadUuid);
        Artefact artefact = dataManagementService.getArtefact(inputPayloadUuid);
        String htmlFile = "";
        JsonNode topLevelNode = new ObjectMapper().readTree(rawJson);
        switch (artefact.getListType()) {
            case CIVIL_DAILY_CAUSE_LIST:
                htmlFile = new CivilDailyCauseListConverter().convert(topLevelNode);
                break;
            case SJP_PUBLIC_LIST:
                htmlFile = new SjpPublicListConverter().convert(topLevelNode);
                break;
            case FAMILY_DAILY_CAUSE_LIST:
                htmlFile = new FamilyDailyCauseListConverter().convert(topLevelNode);
                break;
            case SJP_PRESS_LIST:
                htmlFile = new SjpPressListConverter().convert(topLevelNode);
                break;
            default:
                //Throw exception
                break;
            //todo add mixed lists
        }
        return generatePdfFromHtml(htmlFile);
    }

    private String sjpPublicListTemplate(JsonNode json) throws JsonProcessingException {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        String date = json.get("document").get("publicationDate").asText();

        context.setVariable("date", date);
        context.setVariable("jsonBody", json);

        Map<String, String> cases = new HashMap<>();
        cases.put("Josh", "hello");
        cases.put("Junaid", "hi");
        cases.put("Danny", "你好");
        cases.put("Kian", "ahoy!");
        cases.put("Chris", "hola");
        cases.put("Nigel", "bonjour");
        context.setVariable("cases", cases);
        return templateEngine.process("sjpPublicList.html", context);
    }


    /**
     * Class which takes in JSON input and uses it to inform a given template. Consider this a placeholder until we
     * have specific style guides created.
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
     * Class which takes in an HTML file and generates an accessible PDF file (as a byteArray).
     * @param html - string input representing a well-formed HTML file conforming to WCAG pdf accessibility guidance
     * @return a byte array representing the generated PDF.
     * @throws IOException - if errors appear during the process.
     */
    public byte[] generatePdfFromHtml(String html) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePdfUaAccessbility(true);
            builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U);
            File ourFont = new ClassPathResource(
                "gdsFont.otf").getFile();
            builder.useFont(ourFont, "GDS Transport");
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        }
    }
}
