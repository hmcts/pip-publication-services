package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactCaseInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseInfoHelperTest {

    private static final String CASE_NUMBER_VALUE = "12345678";
    private static final String CASE_NAME_VALUE = "This is a case name";
    private static final String CASE_MATCH_MESSAGE = "Returned case value does not match";

    @Test
    void testGenerateCasePersonalisationFromCaseNumberWithCaseName() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNumbers(artefact,
                                                                                         List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")");
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNumberWithNoCaseName() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNumbers(artefact,
                                                                                         List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE);
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNumberWithNoCaseInfo() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNumbers(artefact,
                                                                                         List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE);
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNumberWithNoCaseSubscriptionType() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNumbers(artefact, List.of());

        assertThat(results)
            .as(CASE_MATCH_MESSAGE)
            .isEmpty();
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNameWithCaseNumber() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNames(artefact,
                                                                                       List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")");
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNameWithNoCaseNumber() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNames(artefact,
                                                                                       List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NAME_VALUE);
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNameWithNoCaseInfo() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNames(artefact,
                                                                                       List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NAME_VALUE);
    }

    @Test
    void testGenerateCasePersonalisationFromCaseNameWithNoCaseNameSubscriptionType() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCasePersonalisationFromCaseNames(artefact, List.of());

        assertThat(results)
            .as(CASE_MATCH_MESSAGE)
            .isEmpty();
    }
}
