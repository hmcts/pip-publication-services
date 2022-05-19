package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.publication.services.client.EmailClient;
import uk.gov.hmcts.reform.pip.publication.services.config.NotifyConfigProperties;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.request.CreatedAdminWelcomeEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles any personalisation for the emails.
 */
@Component
@Slf4j
public class PersonalisationService {

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    NotifyConfigProperties notifyConfigProperties;

    private static final String SUBSCRIPTION_PAGE_LINK = "subscription_page_link";
    private static final String START_PAGE_LINK = "start_page_link";
    private static final String GOV_GUIDANCE_PAGE_LINK = "gov_guidance_page";
    private static final String AAD_SIGN_IN_LINK = "sign_in_page_link";
    private static final String AAD_RESET_LINK = "reset_password_link";
    private static final String SURNAME = "surname";
    private static final String FORENAME = "first_name";
    private static final String CASE_NUMBERS = "case_num";
    private static final String DISPLAY_CASE_NUMBERS = "display_case_num";
    private static final String CASE_URN = "case_urn";
    private static final String DISPLAY_CASE_URN = "display_case_urn";
    private static final String LOCATIONS = "locations";
    private static final String DISPLAY_LOCATIONS = "display_locations";
    private static final String YES = "Yes";
    private static final String NO = "No";


    /**
     * Handles the personalisation for the Welcome email.
     * @return The personalisation map for the welcome email.
     */
    public Map<String, Object> buildWelcomePersonalisation() {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(SUBSCRIPTION_PAGE_LINK, notifyConfigProperties.getLinks().getSubscriptionPageLink());
        personalisation.put(START_PAGE_LINK, notifyConfigProperties.getLinks().getStartPageLink());
        personalisation.put(GOV_GUIDANCE_PAGE_LINK, notifyConfigProperties.getLinks().getGovGuidancePageLink());
        return personalisation;
    }

    /**
     * Handles the personalisation for the admin account email
     * @param body The body of the admin email.
     * @return The personalisation map for the admin account email.
     */
    public Map<String, Object> buildAdminAccountPersonalisation(CreatedAdminWelcomeEmail body) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        personalisation.put(SURNAME, body.getSurname());
        personalisation.put(FORENAME, body.getForename());
        personalisation.put(AAD_RESET_LINK, notifyConfigProperties.getLinks().getAadPwResetLink());
        personalisation.put(AAD_SIGN_IN_LINK, notifyConfigProperties.getLinks().getAadSignInPageLink());
        return personalisation;
    }

    /**
     * Handles the personalisation for the raw data subscription email
     * @param body The body of the subscription.
     * @param artefact The artefact to send in the subscription.
     * @return The personalisation map for the raw data subscription email.
     */
    //TODO: This method is not used and will be updated once JSON file subscription tickets have been played, however
    //TODO: provided as a placeholder for now
    public Map<String, Object> buildRawDataSubscriptionPersonalisation(SubscriptionEmail body,
                                                                        Artefact artefact) {
        Map<String, Object> personalisation = new ConcurrentHashMap<>();
        body.getSubscriptions().forEach((key, value) -> {
            switch (key) {
                case CASE_NUMBER:
                    populateGenericPersonalisation(personalisation, DISPLAY_CASE_NUMBERS, CASE_NUMBERS, value);
                    break;
                case CASE_URN:
                    populateGenericPersonalisation(personalisation, DISPLAY_CASE_URN, CASE_URN, value);
                    break;
                case LOCATION_ID:
                    populateLocationPersonalisation(personalisation, value);
                    break;
            }
        });

        personalisation.put("list_type", artefact.getListType());
        personalisation.put("link_to_file", "<Placeholder>");
        personalisation.put("testing_of_array", "<Placeholder>");

        return personalisation;
    }

    /**
     * Handles the personalisation for the flat file subscription email
     * @param body The body of the subscription.
     * @param artefact The artefact to send in the subscription.
     * @return The personalisation map for the flat file subscription email.
     */
    public Map<String, Object> buildFlatFileSubscriptionPersonalisation(SubscriptionEmail body,
                                                                         Artefact artefact) {
        try {
            Map<String, Object> personalisation = new ConcurrentHashMap<>();
            List<String> location = body.getSubscriptions().get(SubscriptionTypes.LOCATION_ID);
            populateLocationPersonalisation(personalisation, location);

            personalisation.put("list_type", artefact.getListType());

            byte[] artefactData = dataManagementService.getArtefactFlatFile(body.getArtefactId());
            personalisation.put("link_to_file", EmailClient.prepareUpload(artefactData));

            return personalisation;
        } catch (NotificationClientException e) {
            log.warn("Error adding attachment to flat file email {}. Artefact ID: {}", body.getEmail(),
                     artefact.getArtefactId());
            throw new NotifyException(e.getMessage());
        }
    }

    private void populateGenericPersonalisation(Map<String, Object> personalisation, String display,
                                         String displayValue, List<String> content) {
        if (content.size() > 0) {
            personalisation.put(display, YES);
            personalisation.put(displayValue, content);
        } else {
            personalisation.put(display, NO);
            personalisation.put(displayValue, "");
        }
    }

    private void populateLocationPersonalisation(Map<String, Object> personalisation, List<String> content) {
        if (content != null && content.size() > 0) {
            Location subLocation = dataManagementService.getLocation(content.get(0));
            personalisation.put(DISPLAY_LOCATIONS, YES);
            personalisation.put(LOCATIONS, subLocation.getName());
        } else {
            personalisation.put(DISPLAY_LOCATIONS, NO);
            personalisation.put(LOCATIONS, "");
        }
    }

}
