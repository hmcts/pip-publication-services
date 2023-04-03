package uk.gov.hmcts.reform.pip.publication.services.helpers;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.CaseSearch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseNameHelperTest {

    private static final String CASE_NUMBER_VALUE = "12345678";
    private static final String CASE_NAME_VALUE = "This is a case name";

    private static final String CASES = "cases";

    private CaseNameHelper caseNameHelper = new CaseNameHelper();

    @Test
    void testWithSearchNull() {
        Artefact artefact = new Artefact();
        artefact.setSearch(null);

        List<String> returnedCaseNumbers =
            caseNameHelper.generateCaseNumberPersonalisation(artefact, List.of(CASE_NUMBER_VALUE));

        assertEquals(List.of(CASE_NUMBER_VALUE), returnedCaseNumbers, "Case number not as expected");
    }

    @Test
    void testWithCaseNamePresent() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseName(CASE_NAME_VALUE);
        caseSearch.setCaseNumber(CASE_NUMBER_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, CASE_NAME_VALUE);
    }

    @Test
    void testWithSearchPresentButNoCaseName() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber(CASE_NUMBER_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    @Test
    void testWithSearchPresentButDoesNotContainCases() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, "cases2", null);
    }

    @Test
    void testWithCaseNumberPresentButIsEmpty() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber("");
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    @Test
    void testWithCaseNumberPresentButDoesNotMatch() {
        CaseSearch caseSearch = new CaseSearch();
        caseSearch.setCaseNumber("11111111");
        caseSearch.setCaseName(CASE_NAME_VALUE);

        assertCaseNamePersonalisationIsCorrect(caseSearch, CASES, null);
    }

    private void assertCaseNamePersonalisationIsCorrect(CaseSearch caseSearch, String caseValue, String caseName) {
        Map<String, List<Object>> searchCriteria = new ConcurrentHashMap<>();
        searchCriteria.put(caseValue, List.of(caseSearch));

        Artefact artefact = new Artefact();
        artefact.setSearch(searchCriteria);

        List<String> returnedCaseNumbers =
            caseNameHelper.generateCaseNumberPersonalisation(artefact, List.of(CASE_NUMBER_VALUE));

        if (Strings.isNullOrEmpty(caseName)) {
            assertEquals(List.of(CASE_NUMBER_VALUE), returnedCaseNumbers, "Case number not as expected");
        } else {
            assertEquals(List.of(CASE_NUMBER_VALUE + " (" + CASE_NAME_VALUE + ")"), returnedCaseNumbers,
                         "Case number not as expected");
        }
    }
}
