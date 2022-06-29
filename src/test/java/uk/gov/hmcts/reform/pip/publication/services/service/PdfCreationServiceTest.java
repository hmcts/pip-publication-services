package uk.gov.hmcts.reform.pip.publication.services.service;

import com.openhtmltopdf.util.XRRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
class PdfCreationServiceTest {

    @Mock
    private DataManagementService dataManagementService;

    @InjectMocks
    private PdfCreationService pdfCreationService;

    @Test
    void testPdfGenerationSuccess() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Files.newInputStream(Paths.get("src/test/resources/mocks/", "testThyme.html")), writer,
                     Charset.defaultCharset()
        );

        byte[] outputPdf = pdfCreationService.generatePdfFromHtml(writer.toString());

        try (PDDocument doc = PDDocument.load(outputPdf)) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            String outputText = pdfTextStripper.getText(doc);

            assertTrue(outputText.contains(
                "An example file for the creation of PDF lists from our JSON artefact payloads"
                    + "."), "Output pdf does not contain input text");

            assertEquals(doc.getNumberOfPages(), 5, "Output pdf is not the correct length");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Test
    void testPdfGenerationFailure() {
        String badInputHtml = "1 2 3 4 broken html";
        assertThrows(XRRuntimeException.class, () ->
            pdfCreationService.generatePdfFromHtml(badInputHtml), "Exception not thrown");
    }

    @Test
    void testJsontoHtmltoPdf() throws IOException {
        UUID uuid = UUID.randomUUID();
        String inputJson = "{\"document\":{\"value1\":\"x\",\"value2\":\"hiddenTestString\"}}";
        when(dataManagementService.getArtefactJsonPayload(uuid)).thenReturn(inputJson);

        byte[] outputPdf = pdfCreationService.jsonToPdf(uuid);
        try (PDDocument doc = PDDocument.load(outputPdf)) {
            assertEquals(doc.getNumberOfPages(), 1, "pages not correct");
            PDFTextStripper stripper = new PDFTextStripper();
            assertTrue(stripper.getText(doc).contains("hiddenTestString"), "hidden text non-existent in final file");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
