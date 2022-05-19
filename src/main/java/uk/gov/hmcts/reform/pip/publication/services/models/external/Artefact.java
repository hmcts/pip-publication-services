package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Data;

import java.util.UUID;

/**
 * Class that represents the Inbound artifact that is being published.
 */

@Data
public class Artefact {

    /**
     * Unique ID for publication.
     */
    private UUID artefactId;

    /**
     * The type of list.
     */
    private ListType listType;

    /**
     * Court Id based on the source system (provenance).
     */
    private String locationId;

    /**
     * Bool to signal if the payload is a flat file or raw data.
     */
    private Boolean isFlatFile = false;

}
