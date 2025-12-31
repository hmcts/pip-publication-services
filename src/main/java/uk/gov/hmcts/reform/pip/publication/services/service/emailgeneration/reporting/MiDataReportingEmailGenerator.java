package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.MiDataReportingEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.EmailGenerator;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MI_DATA_REPORTING_EMAIL;
import static uk.gov.service.notify.NotificationClient.prepareUpload;

/**
 * Generate the MI data reporting email with personalisation for GOV.UK Notify template.
 */
@Service
@Slf4j
public class MiDataReportingEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        MiDataReportingEmailData emailData = (MiDataReportingEmailData) email;
        return generateEmail(emailData, MI_DATA_REPORTING_EMAIL.getTemplate(), buildEmailPersonalisation(emailData));
    }

    private Map<String, Object> buildEmailPersonalisation(MiDataReportingEmailData emailData) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        try {
            personalisation.put("link_to_file",
                                prepareUpload(emailData.getExcel(), false,
                                              emailData.getFileRetentionWeeks()));
            personalisation.put("env_name", convertEnvironmentName(emailData.getEnvName()));
        } catch (NotificationClientException e) {
            log.warn(writeLog("Error adding attachment to MI data reporting email"));
            throw new NotifyException(e.getMessage());
        }
        return personalisation;
    }
}
