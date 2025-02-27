package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.request.BulkSubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.utils.IntegrationTestBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
class NotifySubscriptionTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BULK_SUBSCRIPTION_URL = "/notify/v2/subscription";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String EMAIL = "test@justice.gov.uk";
    private static final byte[] FILE = "Test byte".getBytes();
    private static final String LOCATION_ID = "999";
    private static final String LOCATION_NAME = "Test court";
    private static final String PAYLOAD = "Test JSON";
    private static final String PDF = "Test PDF";

    private static final String BULK_SUBSCRIPTION_EMAIL_BODY_BAD_REQUEST = """
        {
           "artefactId":"b190522a-5d9b-4089-a8c8-6918721c93df",
           "subscriptionEmails":[
                 "email":"test1@justice.gov.uk",
                 "email":"test2@justice.gov.uk"
           ]
        }
        """;

    private final SubscriptionEmail subscriptionEmail = new SubscriptionEmail();
    private final BulkSubscriptionEmail bulkSubscriptionEmail = new BulkSubscriptionEmail();
    private final Artefact artefact = new Artefact();
    private final Location location = new Location();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setSubscriptions(Map.of(SubscriptionTypes.LOCATION_ID, List.of(LOCATION_ID)));

        bulkSubscriptionEmail.setArtefactId(ARTEFACT_ID);
        bulkSubscriptionEmail.setSubscriptionEmails(List.of(subscriptionEmail));

        artefact.setArtefactId(ARTEFACT_ID);
        artefact.setLocationId(LOCATION_ID);

        location.setLocationId(Integer.parseInt(LOCATION_ID));
        location.setName(LOCATION_NAME);
    }

    @Test
    void testMissingEmailForSubscriptionReturnsBadRequest() throws Exception {
        String missingEmailJsonBody =
            "{\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}, \"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(missingEmailJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testValidPayloadForSubsEmailThrowsBadGateway() throws Exception {
        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenThrow(ServiceToServiceException.class);

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(bulkSubscriptionEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadGateway());
    }

    @Test
    void testInvalidEmailForSubscriptionReturnsBadRequest() throws Exception {
        String invalidEmailJsonBody =
            "{\"email\":\"abcd\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]},"
                + "\"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(invalidEmailJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingArtefactIdForSubscriptionReturnsBadRequest() throws Exception {
        String missingArtefactIdJsonBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[\"0\"]}}";

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(missingArtefactIdJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidSubscriptionCriteriaForSubscriptionReturnsBadRequest() throws Exception {
        String invalidSubscriptionJsonBody =
            "{\"email\":\"test_account_admin@justice.gov.uk\",\"subscriptions\": {\"LOCATION_ID\":[]},"
                + "\"artefactId\": \"3d498688-bbad-4a53-b253-a16ddf8737a9\"}";

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(invalidSubscriptionJsonBody)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendBulkFlatFileEmail() throws Exception {
        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(FILE);

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(bulkSubscriptionEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendBulkJsonEmail() throws Exception {
        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID)).thenReturn(location);
        when(dataManagementService.getArtefactJsonBlob(ARTEFACT_ID)).thenReturn(PAYLOAD);
        when(dataManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false)).thenReturn(PDF);

        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(bulkSubscriptionEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendBulkEmailBadRequest() throws Exception {
        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(BULK_SUBSCRIPTION_EMAIL_BODY_BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "unauthorized_username", authorities = {"APPROLE_unknown.role"})
    void testUnauthorizedSendSubscriptionEmail() throws Exception {
        mockMvc.perform(post(BULK_SUBSCRIPTION_URL)
                            .content(OBJECT_MAPPER.writeValueAsString(bulkSubscriptionEmail))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
