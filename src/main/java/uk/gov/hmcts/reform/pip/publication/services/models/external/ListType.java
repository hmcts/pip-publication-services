package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Getter;

/**
 * Enum that represents the different list types.
 */
@Getter
public enum ListType {
    SJP_PUBLIC_LIST,
    SJP_PRESS_LIST,
    SJP_PRESS_REGISTER,
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGISTRATES_PUBLIC_LIST,
    MAGISTRATES_STANDARD_LIST,
    IAC_DAILY_LIST,
    CIVIL_DAILY_CAUSE_LIST,
    FAMILY_DAILY_CAUSE_LIST,
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST,
    COP_DAILY_CAUSE_LIST,
    SSCS_DAILY_LIST,
    SSCS_DAILY_LIST_ADDITIONAL_HEARINGS,
    PRIMARY_HEALTH_LIST,
    CARE_STANDARDS_LIST,
    ET_DAILY_LIST,
    ET_FORTNIGHTLY_PRESS_LIST;
}
