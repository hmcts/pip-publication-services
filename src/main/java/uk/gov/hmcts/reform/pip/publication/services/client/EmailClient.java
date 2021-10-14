package uk.gov.hmcts.reform.pip.publication.services.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.service.notify.NotificationClient;

/**
 * Class to initiate the Gov Notify Client to send emails.
 */
@Component
@Slf4j
public class EmailClient extends NotificationClient {

    /**
     * Constructor to setup client with api key.
     * @param apiKey API key used to connect to GovNotify taken from application.yaml
     */
    @Autowired
    public EmailClient(@Value("${notify.api.key}") String apiKey) {
        super(apiKey);
    }
}
