package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.BatchEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.notify.Templates;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.List;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Component
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class EmailService {
    private final EmailClient emailClient;
    private final RateLimitingService rateLimitingService;
    private final NotifyConfigProperties notifyConfigProperties;

    @Autowired
    public EmailService(EmailClient emailClient, RateLimitingService rateLimitingService,
                        NotifyConfigProperties notifyConfigProperties) {
        this.emailClient = emailClient;
        this.rateLimitingService = rateLimitingService;
        this.notifyConfigProperties = notifyConfigProperties;
    }

    public EmailToSend handleEmailGeneration(EmailBody emailBody, Templates emailTemplate) {
        rateLimitingService.validate(emailBody.getEmail(), emailTemplate);
        return emailTemplate.getEmailGenerator()
            .buildEmail(emailBody, notifyConfigProperties.getLinks());
    }

    public List<EmailToSend> handleBatchEmailGeneration(BatchEmailBody emailBody, Templates emailTemplate) {
        List<String> emails = applyEmailRateLimit(emailBody.getEmails(), emailTemplate);
        emailBody.setEmails(emails);
        return emailTemplate.getBatchEmailGenerator()
            .buildEmail(emailBody, notifyConfigProperties.getLinks());
    }

    public SendEmailResponse sendEmail(EmailToSend emailToSend) {
        try {
            SendEmailResponse response = emailClient.sendEmail(emailToSend.getTemplate(), emailToSend.getEmailAddress(),
                                         emailToSend.getPersonalisation(), emailToSend.getReferenceId());
            String emailDescription = Templates.get(emailToSend.getTemplate()).getDescription();
            log.info(writeLog(
                String.format("%s successfully sent with reference ID: %s",
                              emailDescription, emailToSend.getReferenceId())
            ));
            return response;
        } catch (NotificationClientException e) {
            log.error(writeLog(String.format("Failed to send email. "
                                                       + "Reference ID: %s. "
                                                       + "Reason: %s", emailToSend.getReferenceId(), e)));
            throw new NotifyException(e.getMessage());
        }
    }

    private List<String> applyEmailRateLimit(List<String> emails, Templates emailTemplate) {
        return emails.stream()
            .filter(e -> rateLimitingService.isValid(e, emailTemplate))
            .toList();
    }
}
