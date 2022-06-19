package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class PdfCreationService {

    @Autowired
    private DataManagementService dataManagementService;

    public String jsonToHtml(UUID inputPayloadUuid) throws DocumentException, IOException {
        String rawJson = dataManagementService.getArtefactJsonPayload(inputPayloadUuid);
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObj = mapper.readValue(rawJson, Object.class);
        String prettyJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
        String htmlFile = parseThymeleafTemplate(prettyJsonString);
        generatePdfFromHtml(htmlFile);
        return "success";
    }

    private String parseThymeleafTemplate(String json) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        context.setVariable("jsonBody", json);
        return templateEngine.process("testTemplate.html", context);
    }

    public void generatePdfFromHtml(String html) {
        String outputFolder = System.getProperty("user.home") + File.separator + "thymeleaf.pdf";

        try (OutputStream outputStream = Files.newOutputStream(Paths.get(outputFolder))) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (IOException | DocumentException ex) {
            log.error(ex.getMessage());
        }
    }
}
