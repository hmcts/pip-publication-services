package uk.gov.hmcts.reform.pip.publication.services.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.S3UploadException;

import java.io.InputStream;

@Service
public class AwsS3Service {

    @Value("${cloud.aws.s3-bucket-name}")
    private String bucketName;

    private final S3Client s3Client;

    @Autowired
    public AwsS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String key, InputStream fileStream) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.putObject(request, RequestBody.fromContentProvider(() -> fileStream,
                                                                        "application/octet-stream"));
        } catch (S3Exception e) {
            throw new S3UploadException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }
}
