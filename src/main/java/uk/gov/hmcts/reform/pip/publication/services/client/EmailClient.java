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
    public EmailClient(@Value("${notify.api.key}") String apiKey,@Value("${NOTIFY_API_TESTING_KEY}") String kvKey,
    @Value("${notify.links.subscription-page-link}") String link,@Value("${NOTIFY_LINK_SUBSCRIPTION_PAGE}") String envLink) {
        super(apiKey);
        log.warn("CHRIS CHECK HERE FOR THE PRINTED KEY SECRET: " + apiKey + " kvkey: " + kvKey + " link: " + link + " envLink: " + envLink);
    }
}
