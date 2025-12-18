package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.S3UploadException;
import uk.gov.hmcts.reform.pip.publication.services.utils.IntegrationTestBase;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
class NotifyAwsS3BucketTest  extends IntegrationTestBase {
    private static final String AWS_S3_BUCKET_UPLOAD_URL = "/notify/upload-html-to-s3";
    private static final String FORM_FILE_FIELD_NAME = "file";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHtmlUploadToAwsS3BucketSuccess() throws Exception {
        MockMultipartFile htmlFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.html",
            "text/html",
            "<html><body>Test</body></html>".getBytes()
        );

        doNothing().when(awsS3Service).uploadFile(htmlFile.getOriginalFilename(), htmlFile.getInputStream());

        mockMvc.perform(multipart(AWS_S3_BUCKET_UPLOAD_URL)
                            .file(htmlFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(content().string("File uploaded successfully to AWS S3 Bucket"));
    }

    @Test
    void testEmptyHtmlUploadToAwsS3BucketFail() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.html",
            "text/html",
            new byte[0]
        );

        mockMvc.perform(multipart(AWS_S3_BUCKET_UPLOAD_URL)
                            .file(emptyFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("File cannot be empty"));
    }

    @Test
    void testUnsupportedFileUploadToAwsS3BucketFail() throws Exception {
        MockMultipartFile jsonFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.pdf",
            "application/pdf",
            "Test".getBytes()
        );

        mockMvc.perform(multipart(AWS_S3_BUCKET_UPLOAD_URL)
                            .file(jsonFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(content().string("Only HTM/HTML files are allowed"));
    }

    @Test
    void testHtmlUploadToAwsS3BucketInternalSeverError() throws Exception {
        MockMultipartFile htmlFile = new MockMultipartFile(
            FORM_FILE_FIELD_NAME,
            "test.html",
            "text/html",
            "<html><body>Test</body></html>".getBytes()
        );

        doThrow(new S3UploadException("S3 error", new RuntimeException("Root cause")))
            .when(awsS3Service).uploadFile(any(), any(InputStream.class));

        mockMvc.perform(multipart(AWS_S3_BUCKET_UPLOAD_URL)
                            .file(htmlFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Upload failed: S3 error"));
    }
}
