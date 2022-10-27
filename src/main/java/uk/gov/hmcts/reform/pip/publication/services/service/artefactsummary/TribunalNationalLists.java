package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.TribunalNationalList;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.LocationHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.TribunalNationalListsManipulation;

import java.util.List;

@Service
public class TribunalNationalLists {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String artefactSummaryTribunalNationalLists(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder(80);
        JsonNode jsonPayload = OBJECT_MAPPER.readTree(payload);

        LocationHelper.formatCourtAddress(jsonPayload, ", ", true);
        List<TribunalNationalList> tribunalNationalList =
            TribunalNationalListsManipulation.processRawListData(jsonPayload, Language.ENGLISH);

        tribunalNationalList.forEach(data -> {
            output
                .append("•Hearing Date: ")
                .append(data.getHearingDate())
                .append("\n\tCase Name: ")
                .append(data.getCaseName())
                .append("\nDuration: ")
                .append(data.getDuration())
                .append("\nHearing Type: ")
                .append(data.getHearingType())
                .append("\nVenue: ")
                .append(data.getVenue())
                .append('\n');
        });
        return output.toString();
    }
}
