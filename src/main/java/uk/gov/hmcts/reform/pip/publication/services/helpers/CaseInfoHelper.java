package uk.gov.hmcts.reform.pip.publication.services.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.common.base.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactCaseInfo;
import uk.gov.hmcts.reform.pip.publication.services.models.external.CaseSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper class for handling case numbers and names.
 */
@Component
public final class CaseInfoHelper {
    private static final String CASE_FORMAT = "%s (%s)";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CaseInfoHelper() {
    }

    /**
     * Extracts the associated case name to a case number.
     * @param artefact The artefact to extract the case name from.
     * @param content The case numbers that have been searched by.
     * @return The list of case numbers, and case names if available.
     */
    @Deprecated
    public static List<String> generateCaseNumberPersonalisation(Artefact artefact, List<String> content) {
        if (artefact.getSearch() != null && artefact.getSearch().containsKey("cases")) {
            List<CaseSearch> caseSearches = OBJECT_MAPPER.convertValue(artefact.getSearch().get("cases"),
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
                    contentWithCaseNames.add(String.format(CASE_FORMAT, caseNumber, caseName.get()));
                } else {
                    contentWithCaseNames.add(caseNumber);
                }
            });

            return contentWithCaseNames;
        }

        return content;
    }

    /**
     * Extracts the associated case name to a case number.
     * @param artefact The artefact to extract the case names from.
     * @param caseNumbers The case numbers that have been searched by.
     * @return The list of case numbers, and case names if available.
     */
    public static List<String> generateCasePersonalisationFromCaseNumbers(Artefact artefact, List<String> caseNumbers) {
        if (CollectionUtils.isNotEmpty(artefact.getCaseInfoList())) {
            List<String> contentWithCaseNumberAndName = new ArrayList<>();

            caseNumbers.forEach(caseNumber -> {
                Optional<String> caseName = artefact.getCaseInfoList().stream()
                    .filter(caseInfo -> StringUtils.isNotEmpty(caseInfo.getCaseNumber()))
                    .filter(caseInfo -> caseInfo.getCaseNumber().equals(caseNumber))
                    .map(ArtefactCaseInfo::getCaseName)
                    .filter(StringUtils::isNotEmpty)
                    .findFirst();

                if (caseName.isPresent()) {
                    contentWithCaseNumberAndName.add(String.format(CASE_FORMAT, caseNumber, caseName.get()));
                } else {
                    contentWithCaseNumberAndName.add(caseNumber);
                }
            });

            return contentWithCaseNumberAndName;
        }

        return caseNumbers;
    }

    /**
     * Extracts the associated case number to a case name.
     * @param artefact The artefact to extract the case numbers from.
     * @param caseNames The case names that have been searched by.
     * @return The list of case names, and case numbers if available.
     */
    public static List<String> generateCasePersonalisationFromCaseNames(Artefact artefact, List<String> caseNames) {
        if (CollectionUtils.isNotEmpty(artefact.getCaseInfoList())) {
            List<String> contentWithCaseNumberAndName = new ArrayList<>();

            caseNames.forEach(caseName -> {
                Optional<String> caseNumber = artefact.getCaseInfoList().stream()
                    .filter(caseInfo -> StringUtils.isNotEmpty(caseInfo.getCaseName()))
                    .filter(caseInfo -> caseInfo.getCaseName().equals(caseName))
                    .map(ArtefactCaseInfo::getCaseNumber)
                    .filter(StringUtils::isNotEmpty)
                    .findFirst();

                if (caseNumber.isPresent()) {
                    contentWithCaseNumberAndName.add(String.format(CASE_FORMAT, caseNumber.get(), caseName));
                } else {
                    contentWithCaseNumberAndName.add(caseName);
                }
            });

            return contentWithCaseNumberAndName;
        }

        return caseNames;
    }

}
