package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.S3UploadException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AwsS3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private AwsS3Service awsS3Service;

    private static final String TEST_FILE = "test.html";
    private static final String TEST_CONTENT = "test";

    @BeforeEach
    void setup() {
        awsS3Service = new AwsS3Service(s3Client);
    }

    @Test
    void testUploadFileSuccessfully() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        awsS3Service.uploadFile(TEST_FILE, inputStream);

        verify(s3Client, times(1))
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFileThrowsS3UploadExceptionWhenS3Fails() {
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        S3Exception s3Exception =
            (S3Exception) S3Exception.builder().message("S3 error").build();

        doThrow(s3Exception)
            .when(s3Client)
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThrows(
            S3UploadException.class,
            () -> awsS3Service.uploadFile(TEST_FILE, inputStream),
            "Expected S3UploadException when S3Client.putObject throws S3Exception"
        );
    }

    @Test
    void testUploadFileThrowsIoExceptionWhenStreamFails() throws IOException {
        InputStream inputStream = mock(InputStream.class);

        try (inputStream) {
            when(inputStream.available()).thenThrow(new IOException("Stream error"));
            assertThrows(
                IOException.class,
                () -> awsS3Service.uploadFile(TEST_FILE, inputStream),
                "Expected IOException when inputStream.available() throws IOException"
            );
        }
    }
}
