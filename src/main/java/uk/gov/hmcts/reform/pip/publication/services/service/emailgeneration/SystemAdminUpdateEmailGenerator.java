package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.BatchEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.SystemAdminUpdateEmailBody;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.models.Environments.convertEnvironmentName;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.SYSTEM_ADMIN_UPDATE_EMAIL;

@Service
public class SystemAdminUpdateEmailGenerator extends BatchEmailGenerator {
    @Value("${env-name}")
    private String envName;

    @Override
    public List<EmailToSend> buildEmail(BatchEmailBody email, PersonalisationLinks personalisationLinks) {
        SystemAdminUpdateEmailBody emailBody = (SystemAdminUpdateEmailBody) email;
        return generateEmail(emailBody.getEmails(), SYSTEM_ADMIN_UPDATE_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody));
    }

    private Map<String, Object> buildEmailPersonalisation(SystemAdminUpdateEmailBody emailBody) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("requestor_name", emailBody.getRequesterName());
        personalisation.put("attempted/succeeded", emailBody.getActionResult().label.toLowerCase(Locale.ENGLISH));
        personalisation.put("change-type", emailBody.getChangeType().label);
        personalisation.put("Additional_change_detail", emailBody.getAdditionalChangeDetail());
        personalisation.put("env_name", convertEnvironmentName(envName));

        return personalisation;
    }
}
