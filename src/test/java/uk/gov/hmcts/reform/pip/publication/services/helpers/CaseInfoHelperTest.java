package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactCaseInfo;
import uk.gov.hmcts.reform.pip.publication.services.models.external.CaseSearch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseInfoHelperTest {

    private static final String CASE_NUMBER_VALUE = "12345678";
    private static final String CASE_NAME_VALUE = "This is a case name";

    private static final String CASES = "cases";
    private static final String CASE_MATCH_MESSAGE = "Returned case value does not match";

    @Test
    @Deprecated
    void testWithSearchNull() {
        Artefact artefact = new Artefact();
        artefact.setSearch(null);

        List<String> returnedCaseNumbers =
            CaseInfoHelper.generateCaseNumberPersonalisation(artefact, List.of(CASE_NUMBER_VALUE));

        assertEquals(List.of(CASE_NUMBER_VALUE), returnedCaseNumbers, "Case number not as expected");
    }

    @Test
    @Deprecated
    void testWithCaseNamePresent() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseName(CASE_NAME_VALUE);
        caseSearch.setCaseNumber(CASE_NUMBER_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, CASE_NAME_VALUE);
    }

    @Test
    @Deprecated
    void testWithSearchPresentButNoCaseName() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber(CASE_NUMBER_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    @Test
    @Deprecated
    void testWithSearchPresentButDoesNotContainCases() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, "cases2", null);
    }

    @Test
    @Deprecated
    void testWithCaseNumberPresentButIsEmpty() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber("");
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    @Test
    @Deprecated
    void testWithCaseNumberPresentButDoesNotMatch() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber("11111111");
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    @Deprecated
    private void assertCaseNamePersonalisationIsCorrect(CaseSearch caseSearch, String caseValue, String caseName) {
        Map<String, List<Object>> searchCriteria = new ConcurrentHashMap<>();
        searchCriteria.put(caseValue, List.of(caseSearch));

        Artefact artefact = new Artefact();
        artefact.setSearch(searchCriteria);

        List<String> returnedCaseNumbers =
            CaseInfoHelper.generateCaseNumberPersonalisation(artefact, List.of(CASE_NUMBER_VALUE));

        if (Strings.isNullOrEmpty(caseName)) {
            assertEquals(List.of(CASE_NUMBER_VALUE), returnedCaseNumbers, "Case number not as expected");
        } else {
            assertEquals(List.of(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")"), returnedCaseNumbers,
                         "Case number not as expected");
        }
    }

    @Test
    void testGenerateCaseNumberPersonalisationV2WithCaseName() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCaseNumberPersonalisationV2(artefact, List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")");
    }

    @Test
    void testGenerateCaseNumberPersonalisationV2WithNoCaseName() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCaseNumberPersonalisationV2(artefact, List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE);
    }

    @Test
    void testGenerateCaseNumberPersonalisationV2WithNoCaseInfo() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCaseNumberPersonalisationV2(artefact, List.of(CASE_NUMBER_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE);
    }

    @Test
    void testGenerateCaseNumberPersonalisationV2WithNoCaseNumberSubscriptionType() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCaseNumberPersonalisationV2(artefact, List.of());

        assertThat(results)
            .as(CASE_MATCH_MESSAGE)
            .isEmpty();
    }

    @Test
    void testGenerateCaseNamePersonalisationV2WithCaseNumber() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCaseNamePersonalisationV2(artefact, List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")");
    }

    @Test
    void testGenerateCaseNamePersonalisationV2WithNoCaseNumber() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCaseNamePersonalisationV2(artefact, List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NAME_VALUE);
    }

    @Test
    void testGenerateCaseNamePersonalisationV2WithNoCaseInfo() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, "")));

        List<String> results = CaseInfoHelper.generateCaseNamePersonalisationV2(artefact, List.of(CASE_NAME_VALUE));

        assertThat(results.get(0))
            .as(CASE_MATCH_MESSAGE)
            .isEqualTo(CASE_NAME_VALUE);
    }

    @Test
    void testGenerateCaseNamePersonalisationV2WithNoCaseNameSubscriptionType() {
        Artefact artefact = new Artefact();
        artefact.setCaseInfoList(List.of(new ArtefactCaseInfo(CASE_NUMBER_VALUE, CASE_NAME_VALUE)));

        List<String> results = CaseInfoHelper.generateCaseNamePersonalisationV2(artefact, List.of());

        assertThat(results)
            .as(CASE_MATCH_MESSAGE)
            .isEmpty();
    }
}
