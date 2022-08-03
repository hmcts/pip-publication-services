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
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
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
        // todo remove this test or switch it for a better full PDF journey one once mags pubs are in.
        Artefact artefact = new Artefact();
        artefact.setContentDate(LocalDateTime.now());
        artefact.setLocationId("1");
        artefact.setProvenance("france");
        artefact.setListType(ListType.MAGS_PUBLIC_LIST);
        artefact.setLanguage(Language.ENGLISH);
        Location location = new Location();
        location.setName("location");

        UUID uuid = UUID.randomUUID();
        String inputJson = "{\"document\":{\"value1\":\"x\",\"value2\":\"hiddenTestString\"}}";
        when(dataManagementService.getArtefactJsonBlob(uuid)).thenReturn(inputJson);
        when(dataManagementService.getArtefact(uuid)).thenReturn(artefact);
        when(dataManagementService.getLocation("1")).thenReturn(location);

        byte[] outputPdf = pdfCreationService.generatePdfFromHtml(pdfCreationService.jsonToHtml(uuid));
        try (PDDocument doc = PDDocument.load(outputPdf)) {
            assertEquals(doc.getNumberOfPages(), 1, "pages not correct");
            PDFTextStripper stripper = new PDFTextStripper();
            assertTrue(stripper.getText(doc).contains("hiddenTestString"), "hidden text non-existent in final file");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
