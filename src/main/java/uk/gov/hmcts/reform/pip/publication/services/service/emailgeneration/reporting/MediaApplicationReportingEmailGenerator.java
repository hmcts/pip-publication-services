package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MediaApplicationReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_APPLICATION_REPORTING_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

/**
 * Generate the media application reporting email with personalisation for GOV.UK Notify template.
 */
@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class MediaApplicationReportingEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MediaApplicationReportingEmailData emailData = (MediaApplicationReportingEmailData) email;
        return generateEmail(emailData, MEDIA_APPLICATION_REPORTING_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData));
    }

    private Map<String, Object> buildEmailPersonalisation(MediaApplicationReportingEmailData emailData) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            personalisation.put("link_to_file",
                                prepareUpload(emailData.getMediaApplicationsCsv(), false,
                                              emailData.getFileRetentionWeeks()));
            personalisation.put("env_name", convertEnvironmentName(emailData.getEnvName()));
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
