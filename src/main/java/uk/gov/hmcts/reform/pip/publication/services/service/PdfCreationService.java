package uk.gov.hmcts.reform.pip.publication.services.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import uk.gov.hmcts.reform.pip.publication.services.config.ThymeleafConfiguration;

import java.io.ByteArrayOutputStream;
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

    public byte[] jsonToPdf(UUID inputPayloadUuid) throws IOException {
        String rawJson = dataManagementService.getArtefactJsonPayload(inputPayloadUuid);
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObj = mapper.readValue(rawJson, Object.class);
        String prettyJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
        String htmlFile = parseThymeleafTemplate(prettyJsonString);
        return generatePdfFromHtml(htmlFile);
    }

    private String parseThymeleafTemplate(String json) {
        SpringTemplateEngine templateEngine = new ThymeleafConfiguration().templateEngine();
        Context context = new Context();
        context.setVariable("jsonBody", json);
        return templateEngine.process("testTemplate.html", context);
    }

    public byte[] generatePdfFromHtml(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePdfUaAccessbility(true);
            builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U);
            File ourFont = new ClassPathResource(
                "font1.otf").getFile();
            builder.useFont(ourFont, "GDS Transport");
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }
}
