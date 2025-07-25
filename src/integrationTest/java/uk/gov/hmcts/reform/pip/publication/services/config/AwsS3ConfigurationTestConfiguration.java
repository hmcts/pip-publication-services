package uk.gov.hmcts.reform.pip.publication.services.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile({"integration", "integration-rate-limit"})
public class AwsS3ConfigurationTestConfiguration {
    @Bean
    public S3Client s3Client() {
        return Mockito.mock(S3Client.class);
    }
}
