package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Getter;

/**
 * Enum that represents the different list types.
 */
@Getter
public enum ListType {
    SJP_PUBLIC_LIST,
    SJP_PRESS_LIST,
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGS_PUBLIC_LIST,
    MAGS_STANDARD_LIST,
    CIVIL_DAILY_CAUSE_LIST,
    FAMILY_DAILY_CAUSE_LIST

}
