package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.Converter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.CopDailyCauseListConverter;

/**
 * Enum that represents the different list types.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ListType {
    SJP_PUBLIC_LIST,
    SJP_PRESS_LIST,
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGS_PUBLIC_LIST,
    MAGS_STANDARD_LIST,
    CIVIL_DAILY_CAUSE_LIST,
    FAMILY_DAILY_CAUSE_LIST(new CopDailyCauseListConverter()),
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST,
    COP_DAILY_CAUSE_LIST(new CopDailyCauseListConverter()),
    SSCS_DAILY_LIST;

    private Converter converter;
}
