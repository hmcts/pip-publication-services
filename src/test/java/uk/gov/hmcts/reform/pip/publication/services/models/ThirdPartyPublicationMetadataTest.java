package uk.gov.hmcts.reform.pip.publication.services.models;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.time.LocalDateTime;
import java.util.UUID;

@ActiveProfiles("test")
class ThirdPartyPublicationMetadataTest {
    private static final UUID PUBLICATION_ID = UUID.randomUUID();
    private static final String LOCATION_NAME = "Test location name";
    private static final LocalDateTime CONTENT_DATE = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_FROM = LocalDateTime.now();
    private static final LocalDateTime DISPLAY_TO = LocalDateTime.now().plusDays(1);

    @Test
    void testConvertPublicationMetadata() {
        Artefact artefact = new Artefact();
        artefact.setArtefactId(PUBLICATION_ID);
        artefact.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        artefact.setContentDate(CONTENT_DATE);
        artefact.setSensitivity(Sensitivity.PUBLIC);
        artefact.setLanguage(Language.ENGLISH);
        artefact.setDisplayFrom(DISPLAY_FROM);
        artefact.setDisplayTo(DISPLAY_TO);

        ThirdPartyPublicationMetadata thirdPartyPublicationMetadata = new ThirdPartyPublicationMetadata()
            .convert(artefact, LOCATION_NAME);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(thirdPartyPublicationMetadata.getPublicationId()).isEqualTo(PUBLICATION_ID);
        softly.assertThat(thirdPartyPublicationMetadata.getLocationName()).isEqualTo(LOCATION_NAME);
        softly.assertThat(thirdPartyPublicationMetadata.getContentDate()).isEqualTo(CONTENT_DATE);
        softly.assertThat(thirdPartyPublicationMetadata.getSensitivity()).isEqualTo(Sensitivity.PUBLIC);
        softly.assertThat(thirdPartyPublicationMetadata.getLanguage()).isEqualTo(Language.ENGLISH);
        softly.assertThat(thirdPartyPublicationMetadata.getDisplayFrom()).isEqualTo(DISPLAY_FROM);
        softly.assertThat(thirdPartyPublicationMetadata.getDisplayTo()).isEqualTo(DISPLAY_TO);

        softly.assertAll();
    }
}
