package uk.gov.hmcts.pip.publication.services.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.service.notify.NotificationClient;

@Component
public class EmailNotificationClient extends NotificationClient {
    public static final String NOTIFICATION_STATUS = "delivered";
    public static final String NOTIFICATION_TYPE = "email";

    @Autowired
    public EmailNotificationClient(@Value("${NOTIFY_API_KEY}") String apiKey) {
        super(apiKey);
    }
}
