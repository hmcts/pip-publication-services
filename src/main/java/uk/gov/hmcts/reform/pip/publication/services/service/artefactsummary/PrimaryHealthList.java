package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.LocationHelper;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.PrimaryHealthListManipulation;

import java.util.List;

@Service
public class PrimaryHealthList {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String artefactSummaryPrimaryHealthList(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder(80);
        JsonNode jsonPayload = OBJECT_MAPPER.readTree(payload);

        LocationHelper.formatCourtAddress(jsonPayload, ", ", true);
        List<uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.PrimaryHealthList> primaryHealthList =
            PrimaryHealthListManipulation.processRawListData(jsonPayload, Language.ENGLISH);

        primaryHealthList.forEach(data -> {
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
