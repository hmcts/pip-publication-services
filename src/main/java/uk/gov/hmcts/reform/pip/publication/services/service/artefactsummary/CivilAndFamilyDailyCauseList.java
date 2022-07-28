package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.ArtefactSummary;
import uk.gov.hmcts.reform.pip.publication.services.service.helpers.DataManipulation;

@Service
public class CivilAndFamilyDailyCauseList {
    /**
     * Civil and Family cause list parent method - iterates on courtHouse/courtList -
     * if these need to be shown in further
     * iterations, do it here.
     *
     * @param payload - json body.
     * @return - string for output.
     * @throws JsonProcessingException - jackson req.
     */
    public String artefactSummaryCivilAndFamilyDailyCause(String payload)
                                            throws JsonProcessingException {
        JsonNode node = new ObjectMapper().readTree(payload);

        DataManipulation.manipulatedDailyListData(node);

        return ArtefactSummary.processDailyCauseList(node);
    }
}
