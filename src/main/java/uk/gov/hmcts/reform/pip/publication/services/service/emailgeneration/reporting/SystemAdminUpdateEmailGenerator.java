package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.BatchEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.SystemAdminUpdateEmailData;
import uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.BatchEmailGenerator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.SYSTEM_ADMIN_UPDATE_EMAIL;

@Service
/**
 * Generate the system admin update email with personalisation for GOV.UK Notify template.
 */
public class SystemAdminUpdateEmailGenerator extends BatchEmailGenerator {
    @Override
    public List<EmailToSend> buildEmail(BatchEmailData email, PersonalisationLinks personalisationLinks) {
        SystemAdminUpdateEmailData emailData = (SystemAdminUpdateEmailData) email;
        return generateEmail(emailData.getEmails(), SYSTEM_ADMIN_UPDATE_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData), emailData.getReferenceId());
    }

    private Map<String, Object> buildEmailPersonalisation(SystemAdminUpdateEmailData emailData) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("requestor_name", emailData.getRequesterEmail());
        personalisation.put("attempted/succeeded", emailData.getActionResult().label.toLowerCase(Locale.ENGLISH));
        personalisation.put("change-type", emailData.getChangeType().label);
        personalisation.put("Additional_change_detail", emailData.getAdditionalChangeDetail());
        personalisation.put("env_name", convertEnvironmentName(emailData.getEnvName()));

        return personalisation;
    }
}
