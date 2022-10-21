package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers.listmanipulation;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SjpPublicList;

import java.util.Iterator;
import java.util.Optional;

public final class SjpManipulation {
    private static final String ACCUSED = "ACCUSED";
    private static final String PROSECUTOR = "PROSECUTOR";

    private SjpManipulation() {
    }

    public static Optional<SjpPublicList> constructSjpCase(JsonNode hearing) {
        Triple<String, String, String> parties = getCaseParties(hearing.get("party"));
        String offenceTitle = getOffenceTitle(hearing.get("offence"));

        if (StringUtils.isNotBlank(parties.getLeft())
            && StringUtils.isNotBlank(parties.getMiddle())
            && StringUtils.isNotBlank(parties.getRight())
            && StringUtils.isNotBlank(offenceTitle)) {
            return Optional.of(
                new SjpPublicList(parties.getLeft(), parties.getMiddle(), offenceTitle, parties.getRight())
            );
        }

        return Optional.empty();
    }

    private static Triple<String, String, String> getCaseParties(JsonNode partiesNode) {
        String name = null;
        String postcode = null;
        String prosecutor = null;

        for (JsonNode party : partiesNode) {
            String role = party.get("partyRole").asText();
            if (ACCUSED.equals(role)) {
                JsonNode individual = party.get("individualDetails");
                name = buildNameField(individual);
                postcode = individual.get("address").get("postCode").asText();
            } else if (PROSECUTOR.equals(role)) {
                prosecutor = party.get("organisationDetails").get("organisationName").asText();
            }
        }
        return Triple.of(name, postcode, prosecutor);
    }

    private static String buildNameField(JsonNode individual) {
        return individual.get("individualForenames").textValue()
            + " "
            + individual.get("individualSurname").textValue();
    }

    private static String getOffenceTitle(JsonNode offence) {
        StringBuilder output = new StringBuilder();
        Iterator<JsonNode> offenceIterator = offence.elements();
        while (offenceIterator.hasNext()) {
            JsonNode currentOffence = offenceIterator.next();
            if (output.length() == 0) {
                output.append(currentOffence.get("offenceTitle").asText());
            } else {
                output.append(", ").append(currentOffence.get("offenceTitle").asText());
            }
        }
        return output.toString();
    }
}
