package uk.gov.hmcts.reform.pip.publication.services.config;

import org.mockito.Mock;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

@Profile({"integration", "integration-basic"})
public class AwsS3ConfigurationTestConfiguration {
    @Mock
    private S3Client s3Client;
}
