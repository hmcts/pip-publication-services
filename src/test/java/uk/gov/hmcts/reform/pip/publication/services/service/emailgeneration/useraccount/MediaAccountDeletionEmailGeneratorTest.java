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
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.useraccount.MediaAccountDeletionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.MediaDeletionEmail;

import java.util.Map;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.INACTIVE_MEDIA_USER_DELETION_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MediaAccountDeletionEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String FULL_NAME = "Full name";
    private static final String DATE = "11th April 2024";

    private static final String FULL_NAME_PERSONALISATION = "full-name";
    private static final String DATE_PERSONALISATION = "date";
    private static final String LINK_TO_SERVICE_PERSONALISATION = "link-to-service";
    private static final String START_PAGE_LINK_ADDRESS = "http://www.test-link.com";


    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private MediaAccountDeletionEmailGenerator emailGenerator;

    @Test
    void testBuildMediaAccountDeletionEmail() {
        when(personalisationLinks.getStartPageLink()).thenReturn(START_PAGE_LINK_ADDRESS);

        MediaDeletionEmail deletionEmail = new MediaDeletionEmail(FULL_NAME, EMAIL, DATE);
        MediaAccountDeletionEmailData emailData = new MediaAccountDeletionEmailData(deletionEmail);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(INACTIVE_MEDIA_USER_DELETION_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(FULL_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(FULL_NAME);

        softly.assertThat(personalisation.get(DATE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(DATE);

        softly.assertThat(personalisation.get(LINK_TO_SERVICE_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(START_PAGE_LINK_ADDRESS + "/create-media-account");

        softly.assertAll();
    }
}
