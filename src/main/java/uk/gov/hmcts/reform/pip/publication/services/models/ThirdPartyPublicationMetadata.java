package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ThirdPartyPublicationMetadata {
    private UUID publicationId;
    private ListType listType;
    private String locationName;
    private LocalDateTime contentDate;
    private Sensitivity sensitivity;
    private Language language;
    private LocalDateTime displayFrom;
    private LocalDateTime displayTo;

    public ThirdPartyPublicationMetadata convert(Artefact artefact, String locationName) {
        return ThirdPartyPublicationMetadata.builder()
            .publicationId(artefact.getArtefactId())
            .listType(artefact.getListType())
            .locationName(locationName)
            .contentDate(artefact.getContentDate())
            .sensitivity(artefact.getSensitivity())
            .language(artefact.getLanguage())
            .displayFrom(artefact.getDisplayFrom())
            .displayTo(artefact.getDisplayTo())
            .build();
    }
}
