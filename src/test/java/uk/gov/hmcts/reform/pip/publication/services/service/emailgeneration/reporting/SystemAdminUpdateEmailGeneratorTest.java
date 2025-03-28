package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.ChangeType;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.SystemAdminUpdateEmailData;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.SYSTEM_ADMIN_UPDATE_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SystemAdminUpdateEmailGeneratorTest {
    private static final String SYSTEM_ADMIN_EMAIL1 = "systemAdmin1@testing.com";
    private static final String SYSTEM_ADMIN_EMAIL2 = "systemAdmin2@testing.com";
    private static final String ACCOUNT_EMAIL = "testAccountEmail@testing.com";
    private static final String REQUESTER_EMAIL = "test_email@justice.gov.uk";
    private static final String ENV_NAME_ORIGINAL = "stg";
    private static final String ENV_NAME = "Staging";
    private static final String REFERENCE_ID = UUID.randomUUID().toString();

    private static final String REQUESTER_NAME_PERSONALISATION = "requester_email";
    private static final String ATTEMPTED_SUCCEEDED_PERSONALISATION = "attempted/succeeded";
    private static final String CHANGE_TYPE_PERSONALISATION = "change-type";
    private static final String CHANGE_DETAIL_PERSONALISATION = "Additional_change_detail";
    private static final String ENV_NAME_PERSONALISATION = "env_name";

    private static final String RESULTS_MESSAGE = "Returned result size does not match";
    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private SystemAdminUpdateEmailGenerator emailGenerator;

    @Test
    void testBuildSystemAdminUpdateEmail() {
        CreateSystemAdminAction systemAdminAction = new CreateSystemAdminAction();
        systemAdminAction.setRequesterEmail(REQUESTER_EMAIL);
        systemAdminAction.setEmailList(List.of(SYSTEM_ADMIN_EMAIL1, SYSTEM_ADMIN_EMAIL2));
        systemAdminAction.setChangeType(ChangeType.ADD_USER);
        systemAdminAction.setActionResult(ActionResult.ATTEMPTED);
        systemAdminAction.setAccountEmail(ACCOUNT_EMAIL);

        SystemAdminUpdateEmailData emailData = new SystemAdminUpdateEmailData(systemAdminAction, ENV_NAME_ORIGINAL,
                                                                              REFERENCE_ID);
        List<EmailToSend> results = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(results)
            .as(RESULTS_MESSAGE)
            .hasSize(2);

        softly.assertThat(results.get(0).getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(SYSTEM_ADMIN_EMAIL1);

        softly.assertThat(results.get(1).getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(SYSTEM_ADMIN_EMAIL2);

        softly.assertThat(results.get(0).getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(SYSTEM_ADMIN_UPDATE_EMAIL.getTemplate());

        softly.assertThat(results.get(0).getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isEqualTo(REFERENCE_ID);

        Map<String, Object> personalisation = results.get(0).getPersonalisation();

        softly.assertThat(personalisation.get(REQUESTER_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(REQUESTER_EMAIL);

        softly.assertThat(personalisation.get(ATTEMPTED_SUCCEEDED_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ActionResult.ATTEMPTED.name().toLowerCase(Locale.ENGLISH));

        softly.assertThat(personalisation.get(CHANGE_TYPE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ChangeType.ADD_USER.label);

        softly.assertThat(personalisation.get(CHANGE_DETAIL_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo("Attempted to create account for: " + ACCOUNT_EMAIL);

        softly.assertThat(personalisation.get(ENV_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ENV_NAME);

        softly.assertAll();
    }
}
