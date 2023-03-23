package uk.gov.hmcts.reform.pip.publication.services.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.common.base.Strings;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.CaseSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper class for handling case names.
 */
@Component
public final class CaseNameHelper {

    private ObjectMapper objectMapper;

    public CaseNameHelper() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Extracts the associated case name to a case number.
     * @param artefact The artefact to extract the case name from.
     * @param content The case numbers that have been searched by.
     * @return The list of case numbers, and case names if available.
     */
    public List<String> generateCaseNumberPersonalisation(Artefact artefact, List<String> content) {
        if (artefact.getSearch() != null && artefact.getSearch().containsKey("cases")) {
            List<CaseSearch> caseSearches = objectMapper.convertValue(artefact.getSearch().get("cases"),
                                                                      new TypeReference<>() {});

            List<String> contentWithCaseNames = new ArrayList<>();

            content.forEach(caseNumber -> {
                Optional<String> caseName = caseSearches.stream()
                    .filter(caseSearch -> !Strings.isNullOrEmpty(caseSearch.getCaseNumber()))
                    .filter(caseSearch -> caseSearch.getCaseNumber().equals(caseNumber))
                    .map(CaseSearch::getCaseName)
                    .filter(name -> !Strings.isNullOrEmpty(name))
                    .findFirst();

                if (caseName.isPresent()) {
                    contentWithCaseNames.add(String.format("%s (%s)", caseNumber, caseName.get()));
                } else {
                    contentWithCaseNames.add(caseNumber);
                }
            });

            return contentWithCaseNames;
        }

        return content;
    }

}
