package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Getter;

/**
 * Enum that represents the different list types.
 */
@Getter
public enum ListType {
    SJP_PUBLIC_LIST(LocationType.NATIONAL),
    SJP_PRESS_LIST(LocationType.NATIONAL),
    CROWN_DAILY_LIST(LocationType.VENUE),
    CROWN_FIRM_LIST(LocationType.VENUE),
    CROWN_WARNED_LIST(LocationType.VENUE),
    MAGS_PUBLIC_LIST(LocationType.VENUE),
    MAGS_STANDARD_LIST(LocationType.VENUE),
    CIVIL_DAILY_CAUSE_LIST(LocationType.VENUE),
    FAMILY_DAILY_CAUSE_LIST(LocationType.VENUE);

    /**
     * Flag that represents the Location Type level the list displays at.
     */
    private final LocationType listLocationLevel;

    ListType(LocationType listLocationLevel) {
        this.listLocationLevel = listLocationLevel;
    }

}
