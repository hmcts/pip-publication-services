package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.ListType;


@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
/**
 * Service which extracts relevant summary data from each list type to be included in gov.notify emails. For the most
 * part, developing these is a very fiddly process and it doesn't seem like there's much of an easier way. Some
 * refactoring may be helpful as the iteration pattern on JsonNodes is used over and over again.
 */
public class ArtefactSummaryService {

    @Autowired
    FamilyDailyCauseList familyDailyCauseList;

    /**
     * Parent class to route based on list types.
     *
     * @param payload  - json payload
     * @param listType - list type from artefact
     * @return String which is taken in by the personalisationService to populate bullet points at bottom of
     *     subscriptions email templates.
     * @throws JsonProcessingException - jackson prereq.
     */
    public String artefactSummary(String payload, ListType listType) throws JsonProcessingException {
        switch (listType) {
            case SJP_PUBLIC_LIST:
                return "SJP PUBLIC LIST";
            case SJP_PRESS_LIST:
                return "SJP PRESS LIST";
            case CIVIL_DAILY_CAUSE_LIST:
                return "CIVIL DAILY CAUSE LIST";
            case FAMILY_DAILY_CAUSE_LIST:
                return familyDailyCauseList.artefactSummaryFamilyDailyCause(payload);
            default:
                return "";
        }
    }
}
