package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.reporting;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.reporting.UnidentifiedBlobEmailData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.BAD_BLOB_EMAIL;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UnidentifiedBlobEmailGeneratorTest {
    private static final String EMAIL = "test@testing.com";
    private static final String PROVENANCE = "Provenance";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String LOCATION_ID = "1234";
    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACTS = List.of(
        new NoMatchArtefact(ARTEFACT_ID, PROVENANCE, LOCATION_ID)
    );
    private static final String ENV_NAME_ORIGINAL = "stg";
    private static final String ENV_NAME = "Staging";

    private static final String UNMATCHED_IDS_PERSONALISATION = "array_of_ids";
    private static final String ENV_NAME_PERSONALISATION = "env_name";

    private static final String EMAIL_ADDRESS_MESSAGE = "Email address does not match";
    private static final String NOTIFY_TEMPLATE_MESSAGE = "Notify template does not match";
    private static final String REFERENCE_ID_MESSAGE = "Reference ID does not match";
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Mock
    private PersonalisationLinks personalisationLinks;

    @InjectMocks
    private UnidentifiedBlobEmailGenerator emailGenerator;

    @Test
    void testBuildUnidentifiedBlobEmailSuccess() {
        UnidentifiedBlobEmailData emailData = new UnidentifiedBlobEmailData(EMAIL, NO_MATCH_ARTEFACTS,
                                                                            ENV_NAME_ORIGINAL);

        EmailToSend result = emailGenerator.buildEmail(emailData, personalisationLinks);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getEmailAddress())
            .as(EMAIL_ADDRESS_MESSAGE)
            .isEqualTo(EMAIL);

        softly.assertThat(result.getTemplate())
            .as(NOTIFY_TEMPLATE_MESSAGE)
            .isEqualTo(BAD_BLOB_EMAIL.getTemplate());

        softly.assertThat(result.getReferenceId())
            .as(REFERENCE_ID_MESSAGE)
            .isNotNull();

        Map<String, Object> personalisation = result.getPersonalisation();

        softly.assertThat(personalisation.get(UNMATCHED_IDS_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(List.of(LOCATION_ID + " - " + PROVENANCE + " (" + ARTEFACT_ID + ")"));

        softly.assertThat(personalisation.get(ENV_NAME_PERSONALISATION))
            .as(PERSONALISATION_MESSAGE)
            .isEqualTo(ENV_NAME);

        softly.assertAll();
    }
}
