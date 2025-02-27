package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.useraccount;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaDuplicatedAccountEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.DuplicatedMediaEmail;

import java.util.Map;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_DUPLICATE_ACCOUNT_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MediaDuplicatedAccountEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";

    private static final String FULL_NAME_PERSONALISATION = "full_name";
    private static final String SIGN_IN_PAGE_LINK = "sign_in_page_link";
    private static final String SIGN_IN_PAGE_LINK_ADDRESS = "http://www.test-link.com";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private MediaDuplicatedAccountEmailGenerator emailGenerator;

    @Test
    void testBuildMediaDuplicatedAccountEmail() {
        when(personalisationLinks.getAadSignInPageLink()).thenReturn(SIGN_IN_PAGE_LINK_ADDRESS);

        DuplicatedMediaEmail mediaEmail = new DuplicatedMediaEmail();
        mediaEmail.setEmail(EMAIL);
        mediaEmail.setFullName(FULL_NAME);
        MediaDuplicatedAccountEmailData emailData = new MediaDuplicatedAccountEmailData(mediaEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(MEDIA_DUPLICATE_ACCOUNT_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(SIGN_IN_PAGE_LINK))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(SIGN_IN_PAGE_LINK_ADDRESS);

        softly.assertAll();
    }
}
