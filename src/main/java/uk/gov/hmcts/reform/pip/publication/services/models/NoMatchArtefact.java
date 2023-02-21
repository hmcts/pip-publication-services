package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class NoMatchArtefact {

    private UUID artefactId;
    private String provenance;
    private String locationId;
}
