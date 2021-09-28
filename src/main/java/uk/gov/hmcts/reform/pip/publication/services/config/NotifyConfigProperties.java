package uk.gov.hmcts.reform.pip.publication.services.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;

@Data
@Configuration
@ConfigurationProperties(prefix = "notify")
public class NotifyConfigProperties {

    private PersonalisationLinks links;
}
