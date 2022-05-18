package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
     * Name of source system.
     */
    private String provenance;

    /**
     * Unique of ID of what publication is called by source system.
     */
    private String sourceArtefactId;

    /**
     * List / Outcome / Judgement / Status Update.
     */
    private ArtefactType type;

    /**
     * Level of sensitivity of publication.
     */
    private Sensitivity sensitivity;

    /**
     * Language of publication.
     */
    private Language language;

    /**
     * Metadata that will be indexed for searching.
     */
    private Map<String, List<Object>> search;

    /**
     * Date / Time from which the publication will be displayed.
     */
    private LocalDateTime displayFrom;

    /**
     * Date / Time until which the publication will be displayed.
     */
    private LocalDateTime displayTo;

    /**
     * The type of list.
     */
    private ListType listType;

    /**
     * Court Id based on the source system (provenance).
     */
    private String locationId;

    /**
     * Date / Time the publication is referring to.
     */
    private LocalDateTime contentDate;

    /**
     * Bool to signal if the payload is a flat file or raw data.
     */
    private Boolean isFlatFile = false;

    /**
     * The URL for the payload in the Azure Blob Service.
     */
    private String payload;

}
