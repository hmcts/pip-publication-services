package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.CivilDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.Converter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.FamilyDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.SjpPressListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters.SjpPublicListConverter;

/**
 * Enum that represents the different list types.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ListType {
    SJP_PUBLIC_LIST(new SjpPublicListConverter()),
    SJP_PRESS_LIST(new SjpPressListConverter()),
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGS_PUBLIC_LIST,
    MAGS_STANDARD_LIST,
    CIVIL_DAILY_CAUSE_LIST(new CivilDailyCauseListConverter()),
    FAMILY_DAILY_CAUSE_LIST(new FamilyDailyCauseListConverter()),
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST,
    COP_DAILY_CAUSE_LIST,
    SSCS_DAILY_LIST;

    private Converter converter;
}
