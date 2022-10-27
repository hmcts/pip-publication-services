package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.CareStandardsListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.CivilAndFamilyDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.CivilDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.Converter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.CopDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.CrownDailyListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.EtFortnightlyPressListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.FamilyDailyCauseListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.IacDailyListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.PrimaryHealthListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.SjpPressListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.SjpPublicListConverter;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters.SscsDailyListConverter;

/**
 * Enum that represents the different list types.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ListType {
    SJP_PUBLIC_LIST(new SjpPublicListConverter()),
    SJP_PRESS_LIST(new SjpPressListConverter()),
    SJP_PRESS_REGISTER,
    CROWN_DAILY_LIST(new CrownDailyListConverter()),
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGISTRATES_PUBLIC_LIST,
    MAGISTRATES_STANDARD_LIST,
    IAC_DAILY_LIST(new IacDailyListConverter()),
    CIVIL_DAILY_CAUSE_LIST(new CivilDailyCauseListConverter()),
    FAMILY_DAILY_CAUSE_LIST(new FamilyDailyCauseListConverter()),
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST(new CivilAndFamilyDailyCauseListConverter()),
    COP_DAILY_CAUSE_LIST(new CopDailyCauseListConverter()),
    SSCS_DAILY_LIST(new SscsDailyListConverter()),
    PRIMARY_HEALTH_LIST(new PrimaryHealthListConverter()),
    CARE_STANDARDS_LIST(new CareStandardsListConverter()),
    ET_DAILY_LIST,
    ET_FORTNIGHTLY_PRESS_LIST(new EtFortnightlyPressListConverter());

    private Converter converter;
}
