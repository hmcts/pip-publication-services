package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.PartyRoleMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PartyRoleMapperTest {

    private static final String ERR_MSG = "PartyRole Mapper method doesn't seem to be working correctly";
    private static final String APPLICANT_PETITIONER = "APPLICANT_PETITIONER";
    private static final String RESPONDENT = "RESPONDENT";
    private static final String APPLICANT_PETITIONER_REPRESENTATIVE =
        "APPLICANT_PETITIONER_REPRESENTATIVE";
    private static final String RESPONDENT_REPRESENTATIVE =
        "RESPONDENT_REPRESENTATIVE";



    @Test
    void testConvertPartyRoleApplicantMethod() {
        assertThat(PartyRoleMapper.convertPartyRole(APPLICANT_PETITIONER))
            .as(ERR_MSG)
            .isEqualTo(APPLICANT_PETITIONER);
    }

    @Test
    void testConvertPartyRoleRespondentMethod() {
        assertThat(PartyRoleMapper.convertPartyRole(RESPONDENT))
            .as(ERR_MSG)
            .isEqualTo(RESPONDENT);
    }

    @Test
    void testConvertPartyRoleApplicantRepMethod() {
        assertThat(PartyRoleMapper
                    .convertPartyRole(APPLICANT_PETITIONER_REPRESENTATIVE))
            .as(ERR_MSG)
            .isEqualTo(APPLICANT_PETITIONER_REPRESENTATIVE);
    }

    @Test
    void testConvertPartyRoleRespondentRepMethod() {
        assertThat(PartyRoleMapper
                    .convertPartyRole(RESPONDENT_REPRESENTATIVE))
            .as(ERR_MSG)
            .isEqualTo(RESPONDENT_REPRESENTATIVE);
    }

    @Test
    void testConvertPartyRoleApplicantAbrevMethod() {
        assertThat(PartyRoleMapper.convertPartyRole("CLP20"))
            .as(ERR_MSG)
            .isEqualTo(APPLICANT_PETITIONER);
    }

    @Test
    void testConvertPartyRoleApplicantRespAbrevMethod() {
        assertThat(PartyRoleMapper.convertPartyRole("CREP"))
            .as(ERR_MSG)
            .isEqualTo(APPLICANT_PETITIONER_REPRESENTATIVE);
    }

    @Test
    void testConvertPartyRoleRespondentAbrevMethod() {
        assertThat(PartyRoleMapper.convertPartyRole("DEF20"))
            .as(ERR_MSG)
            .isEqualTo(RESPONDENT);
    }

    @Test
    void testConvertPartyRoleRespondentRespAbrevMethod() {
        assertThat(PartyRoleMapper.convertPartyRole("RREP"))
            .as(ERR_MSG)
            .isEqualTo(RESPONDENT_REPRESENTATIVE);
    }
}
