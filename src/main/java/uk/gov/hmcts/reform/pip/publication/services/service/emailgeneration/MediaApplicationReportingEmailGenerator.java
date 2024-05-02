package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MediaApplicationReportingEmailBody;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_APPLICATION_REPORTING_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class MediaApplicationReportingEmailGenerator extends EmailGenerator {
    @Value("${env-name}")
    private String envName;

    @Value("${file-retention-weeks}")
    private int fileRetentionWeeks;

    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MediaApplicationReportingEmailBody emailBody = (MediaApplicationReportingEmailBody) email;
        return generateEmail(emailBody.getEmail(), MEDIA_APPLICATION_REPORTING_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaApplicationReportingEmailBody emailBody) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            personalisation.put("link_to_file",
                                prepareUpload(emailBody.getMediaApplicationsCsv(), false,
                                              new RetentionPeriodDuration(fileRetentionWeeks, ChronoUnit.WEEKS)));
            personalisation.put("env_name", convertEnvironmentName(envName));
            return personalisation;
        } catch (NotificationClientException e) {
            log.error(writeLog(String.format(
                "Error adding the csv attachment to the media application " + "reporting email with error %s",
                e.getMessage()
            )));
            throw new NotifyException(e.getMessage());
        }
    }
}
