package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Language;

public final class PartyRoleHelper {
    public static final String APPLICANT = "applicant";
    public static final String RESPONDENT = "respondent";
    public static final String CLAIMANT = "claimant";
    public static final String CLAIMANT_REPRESENTATIVE = "claimantRepresentative";
    public static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";

    private PartyRoleHelper() {
    }

    public static void findAndManipulatePartyInformation(JsonNode hearing, Language language, Boolean initialised) {
        StringBuilder applicant = new StringBuilder();
        StringBuilder respondent = new StringBuilder();
        StringBuilder claimant = new StringBuilder();
        StringBuilder claimantRepresentative = new StringBuilder();
        StringBuilder prosecutingAuthority = new StringBuilder();

        hearing.get("party").forEach(party -> {
            if (!GeneralHelper.findAndReturnNodeText(party, "partyRole").isEmpty()) {
                switch (PartyRoleMapper.convertPartyRole(party.get("partyRole").asText())) {
                    case "APPLICANT_PETITIONER": {
                        formatPartyNonRepresentative(party, applicant, initialised);
                        break;
                    }
                    case "APPLICANT_PETITIONER_REPRESENTATIVE": {
                        final String applicantPetitionerDetails = createIndividualDetails(party, initialised);
                        if (!applicantPetitionerDetails.isEmpty()) {
                            String advisor = (language == Language.ENGLISH) ? "Legal Advisor: " :
                                "Cynghorydd Cyfreithiol: ";
                            applicant.append(advisor);
                            applicant.append(applicantPetitionerDetails).append(", ");
                        }
                        break;
                    }
                    case "RESPONDENT": {
                        formatPartyNonRepresentative(party, respondent, initialised);
                        formatPartyNonRepresentative(party, prosecutingAuthority, initialised);
                        break;
                    }
                    case "RESPONDENT_REPRESENTATIVE": {
                        respondent.append(respondentRepresentative(language, party, initialised));
                        break;
                    }
                    case "CLAIMANT_PETITIONER": {
                        formatPartyNonRepresentative(party, claimant, initialised);
                        break;
                    }
                    case "CLAIMANT_PETITIONER_REPRESENTATIVE": {
                        formatPartyNonRepresentative(party, claimantRepresentative, initialised);
                        break;
                    }

                    default:
                        break;
                }
            }
        });

        ((ObjectNode) hearing).put(APPLICANT, GeneralHelper.trimAnyCharacterFromStringEnd(applicant.toString()));
        ((ObjectNode) hearing).put(RESPONDENT, GeneralHelper.trimAnyCharacterFromStringEnd(respondent.toString()));
        ((ObjectNode) hearing).put(CLAIMANT, GeneralHelper.trimAnyCharacterFromStringEnd(claimant.toString()));
        ((ObjectNode) hearing).put(CLAIMANT_REPRESENTATIVE,
                                   GeneralHelper.trimAnyCharacterFromStringEnd(claimantRepresentative.toString()));
        ((ObjectNode) hearing).put(PROSECUTING_AUTHORITY,
                                   GeneralHelper.trimAnyCharacterFromStringEnd(prosecutingAuthority.toString()));
    }

    private static String respondentRepresentative(Language language, JsonNode respondentDetails,
                                                   Boolean initialised) {
        StringBuilder builder = new StringBuilder();
        final String details = createIndividualDetails(respondentDetails, initialised);
        if (!respondentDetails.isEmpty()) {
            String advisor = (language == Language.ENGLISH) ? "Legal Advisor: " :
                "Cynghorydd Cyfreithiol: ";
            builder.append(advisor);
            builder.append(details).append(", ");
        }
        return builder.toString();
    }

    private static void formatPartyNonRepresentative(JsonNode party, StringBuilder builder, Boolean initialised) {
        String respondentDetails = createIndividualDetails(party, initialised);
        respondentDetails = respondentDetails
            + GeneralHelper.stringDelimiter(respondentDetails, ", ");
        builder.insert(0, respondentDetails);
    }

    private static String createIndividualDetails(JsonNode party, Boolean initialised) {
        if (party.has("individualDetails")) {
            JsonNode individualDetails = party.get("individualDetails");
            if (initialised) {
                String forename = GeneralHelper.findAndReturnNodeText(individualDetails, "individualForenames");
                String forenameInitial = forename.isEmpty() ? "" : forename.substring(0, 1);
                return (GeneralHelper.findAndReturnNodeText(individualDetails, "title") + " "
                    + forenameInitial + " "
                    + GeneralHelper.findAndReturnNodeText(individualDetails, "individualSurname")).trim();
            } else {
                return (GeneralHelper.findAndReturnNodeText(individualDetails, "title") + " "
                    + GeneralHelper.findAndReturnNodeText(individualDetails, "individualForenames") + " "
                    + GeneralHelper.findAndReturnNodeText(individualDetails, "individualMiddleName") + " "
                    + GeneralHelper.findAndReturnNodeText(individualDetails, "individualSurname")).trim();
            }
        }
        return "";
    }
}
