package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.MiDataReportingEmailBody;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.RetentionPeriodDuration;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MI_DATA_REPORTING_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

@Service
@Slf4j
@SuppressWarnings("PMD.PreserveStackTrace")
public class MiDataReportingEmailGenerator extends EmailGenerator {
    @Value("${env-name}")
    private String envName;

    @Value("${file-retention-weeks}")
    private int fileRetentionWeeks;

    @Value("${notify.pi-team-email}")
    private String piTeamEmail;

    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        MiDataReportingEmailBody emailBody = (MiDataReportingEmailBody) email;
        return generateEmail(emailBody.getEmail(), MI_DATA_REPORTING_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(MiDataReportingEmailBody emailBody) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        try {
            personalisation.put("link_to_file",
                                prepareUpload(emailBody.getExcel(), false,
                                              new RetentionPeriodDuration(fileRetentionWeeks, ChronoUnit.WEEKS)));
            personalisation.put("env_name", convertEnvironmentName(envName));
        } catch (NotificationClientException e) {
            log.warn(writeLog("Error adding attachment to MI data reporting email"));
            throw new NotifyException(e.getMessage());
        }
        return personalisation;
    }
}
