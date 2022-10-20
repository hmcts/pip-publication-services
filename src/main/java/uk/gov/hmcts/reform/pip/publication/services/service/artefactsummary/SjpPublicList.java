package uk.gov.hmcts.reform.pip.publication.services.service.artefactsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation.SjpManipulation;

import java.util.Optional;

@Service
public class SjpPublicList {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HEARING = "hearing";
    private static final String COURT_LISTS = "courtLists";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";

    /**
     * parent method for sjp public lists. iterates on sittings.
     *
     * @param payload - json body.
     * @return string of data.
     * @throws JsonProcessingException - jackson prereq.
     */
    public String artefactSummarySjpPublic(String payload) throws JsonProcessingException {
        StringBuilder output = new StringBuilder();

        OBJECT_MAPPER.readTree(payload).get(COURT_LISTS).forEach(courtList -> {
            courtList.get(COURT_HOUSE).get(COURT_ROOM).forEach(courtRoom -> {
                courtRoom.get(SESSION).forEach(session -> {
                    session.get(SITTINGS).forEach(sitting -> {
                        sitting.get(HEARING).forEach(hearing -> {
                            Optional<uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPublicList>
                                sjpCaseOptional = SjpManipulation.constructSjpCase(hearing);
                            if (sjpCaseOptional.isPresent()) {
                                uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPublicList
                                    sjpCase = sjpCaseOptional.get();
                                output
                                    .append("•Defendant: ")
                                    .append(sjpCase.getName())
                                    .append("\nPostcode: ")
                                    .append(sjpCase.getPostcode())
                                    .append("\nProsecutor: ")
                                    .append(sjpCase.getProsecutor())
                                    .append("\nOffence: ")
                                    .append(sjpCase.getOffence())
                                    .append('\n');
                            }
                        });
                    });
                });
            });
        });
        return output.toString();
    }
}
