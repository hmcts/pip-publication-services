package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.AdminWelcomeEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.EmailData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.ADMIN_ACCOUNT_CREATION_EMAIL;

@Service
public class AdminWelcomeEmailGenerator extends EmailGenerator {
    @Override
    public EmailToSend buildEmail(EmailData email, PersonalisationLinks personalisationLinks) {
        AdminWelcomeEmailData emailData = (AdminWelcomeEmailData) email;
        return generateEmail(emailData.getEmail(),
                             ADMIN_ACCOUNT_CREATION_EMAIL.getTemplate(),
                             buildEmailPersonalisation(emailData, personalisationLinks));
    }

    private Map<String, Object> buildEmailPersonalisation(AdminWelcomeEmailData emailData,
                                                          PersonalisationLinks personalisationLinks) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put("first_name", emailData.getForename());
        personalisation.put("reset_password_link", personalisationLinks.getAadPwResetLinkAdmin());
        personalisation.put("admin_dashboard_link", personalisationLinks.getAdminDashboardLink());
        return personalisation;
    }
}
