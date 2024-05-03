package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.models.PersonalisationLinks;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.UnidentifiedBlobEmailData;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.BAD_BLOB_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class UnidentifiedBlobEmailGeneratorTest extends RedisConfigurationTestBase {
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
    private static final String PERSONALISATION_MESSAGE = "Personalisation does not match";

    @Autowired
    private NotifyConfigProperties notifyConfigProperties;

    @Autowired
    private UnidentifiedBlobEmailGenerator emailGenerator;

    @Test
    void testBuildUnidentifiedBlobEmailSuccess() {
        PersonalisationLinks personalisationLinks = notifyConfigProperties.getLinks();
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
