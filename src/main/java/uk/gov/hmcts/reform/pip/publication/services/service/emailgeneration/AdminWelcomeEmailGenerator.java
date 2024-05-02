package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.AdminWelcomeEmailBody;
import uk.gov.hmcts.reform.pip.publication.services.models.emailbody.EmailBody;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.ADMIN_ACCOUNT_CREATION_EMAIL;

@Service
public class AdminWelcomeEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailBody email, PersonalisationLinks personalisationLinks) {
        AdminWelcomeEmailBody emailBody = (AdminWelcomeEmailBody) email;
        return generateEmail(emailBody.getEmail(),
                             ADMIN_ACCOUNT_CREATION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailBody, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(AdminWelcomeEmailBody emailBody,
                                                         PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("first_name", emailBody.getForename());
        personalisation.put("reset_password_link", personalisationLinks.getAadPwResetLinkAdmin());
        personalisation.put("admin_dashboard_link", personalisationLinks.getAdminDashboardLink());
        return personalisation;
    }
}
