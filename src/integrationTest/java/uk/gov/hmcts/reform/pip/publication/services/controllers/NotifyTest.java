package uk.gov.hmcts.reform.pip.publication.services.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import uk.gov.hmcts.reform.pip.publication.services.models.MediaApplication;
import uk.gov.hmcts.reform.pip.publication.services.models.NoMatchArtefact;
import uk.gov.hmcts.reform.pip.publication.services.utils.IntegrationTestBase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods"})
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@ActiveProfiles("integration")
class NotifyTest extends IntegrationTestBase {

    private static final String VALID_WELCOME_REQUEST_BODY_EXISTING = """
        {
            "email": "test@email.com",
            "isExisting": "true",
            "fullName": "fullName"
        }
        """;

    private static final String VALID_WELCOME_REQUEST_BODY_NEW = """
        {
            "email": "test@email.com",
            "isExisting": "false",
            "fullName": "fullName"
        }
        """;

    private static final String VALID_ADMIN_CREATION_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "surname": "surname",
            "forename": "forename"
        };
        """;

    private static final String INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "isExisting":
        }
        """;

    private static final String VALID_DUPLICATE_MEDIA_REQUEST_BODY = """
        {
            "email": "test@email.com",
            "fullName": "fullName"
        };
        """;

    private static final String DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY = """
        {
            "email": "test@email.com",
            "fullName":
        }
        """;

    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY = """
        {
            "locationName": "Test Location",
            "subscriberEmails": [
                "test.system.admin@justice.gov.uk"
            ]
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL = """
        {
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_RESULT = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "changeType": "DELETE_LOCATION",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_TYPE = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "emailList": [
                "test.system.admin@justice.gov.uk"
            ],
            "detailString": "test"
        }
        """;

    private static final String NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL_LIST = """
        {
            "requesterEmail": "test_user@justice.gov.uk",
            "actionResult": "ATTEMPTED",
            "changeType": "DELETE_LOCATION",
            "detailString": "test"
        }
        """;

    private static final String VALID_MEDIA_VERIFICATION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@email.com"
        }
        """;

    private static final String VALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String INVALID_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test@justice.gov.uk",
            "reasons": "invalid"
        }
         """;

    private static final String INVALID_NOTIFY_MEDIA_REJECTION_EMAIL_BODY = """
        {
            "fullName": "fullName",
            "email": "test",
            "reasons": {
                "noMatch": [
                    "Details provided do not match.",
                    "The name, email address and Press ID do not match each other."
                ]
            }
        }
         """;

    private static final String VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY = """
        {
            "email": "test@test.com",
            "fullName": "testName",
            "lastSignedInDate": "01 May 2022"
        }
        """;

    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String ADMIN_CREATED_WELCOME_EMAIL_URL = "/notify/created/admin";
    private static final String MEDIA_REPORTING_EMAIL_URL = "/notify/media/report";
    private static final String MI_REPORTING_EMAIL_URL = "/notify/mi/report";
    private static final String UNIDENTIFIED_BLOB_EMAIL_URL = "/notify/unidentified-blob";
    private static final String MEDIA_VERIFICATION_EMAIL_URL = "/notify/media/verification";
    private static final String MEDIA_REJECTION_EMAIL_URL = "/notify/media/reject";
    private static final String INACTIVE_USER_NOTIFICATION_EMAIL_URL = "/notify/user/sign-in";
    private static final String NOTIFY_SYSTEM_ADMIN_URL = "/notify/sysadmin/update";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL = "/notify/location-subscription-delete";
    private static final String DUPLICATE_MEDIA_EMAIL_URL = "/notify/duplicate/media";

    private static final UUID ID = UUID.randomUUID();
    private static final String ID_STRING = UUID.randomUUID().toString();
    private static final String FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String STATUS = "APPROVED";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String IMAGE_NAME = "test-image.png";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_username";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.role";
    private static final String INVALID_CONTENT = "invalid content";

    private static final List<MediaApplication> MEDIA_APPLICATION_LIST =
        List.of(new MediaApplication(ID, FULL_NAME, EMAIL, EMPLOYER,
                                     ID_STRING, IMAGE_NAME, DATE_TIME, STATUS, DATE_TIME
        ));


    private static final List<NoMatchArtefact> NO_MATCH_ARTEFACT_LIST = new ArrayList<>();

    private String validMediaReportingJson;
    private String validLocationsListJson;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws IOException {
        NO_MATCH_ARTEFACT_LIST.add(new NoMatchArtefact(
            UUID.randomUUID(),
            "TEST",
            "1234"
        ));

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();

        validMediaReportingJson = ow.writeValueAsString(MEDIA_APPLICATION_LIST);
        validLocationsListJson = ow.writeValueAsString(NO_MATCH_ARTEFACT_LIST);
    }

    @Test
    void testValidPayloadReturnsSuccessExisting() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testValidPayloadReturnsSuccessNew() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_NEW)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendDuplicateMediaAccountEmailBadRequest() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(DUPLICATE_MEDIA_EMAIL_INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDuplicateMediaAccountEmail() throws Exception {
        mockMvc.perform(post(DUPLICATE_MEDIA_EMAIL_URL)
                            .content(VALID_DUPLICATE_MEDIA_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendAdminAccountWelcomeEmailBadRequest() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(INVALID_JSON_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendAdminAccountWelcomeEmail() throws Exception {
        mockMvc.perform(post(ADMIN_CREATED_WELCOME_EMAIL_URL)
                            .content(VALID_ADMIN_CREATION_REQUEST_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendWelcomeEmail() throws Exception {
        mockMvc.perform(post(WELCOME_EMAIL_URL)
                            .content(VALID_WELCOME_REQUEST_BODY_EXISTING)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendMediaReportingEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaReportingEmail() throws Exception {
        mockMvc.perform(post(MEDIA_REPORTING_EMAIL_URL)
                            .content(validMediaReportingJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendUnidentifiedBlobEmailBadRequest() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendUnidentifiedBlobEmail() throws Exception {
        mockMvc.perform(post(UNIDENTIFIED_BLOB_EMAIL_URL)
                            .content(validLocationsListJson)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testValidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMediaUserVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(VALID_MEDIA_VERIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidPayloadMediaVerificationEmail() throws Exception {
        mockMvc.perform(post(MEDIA_VERIFICATION_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMediaUserRejectionEmail() throws Exception {

        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendMediaUserRejectionEmailBadRequest() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(INVALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testSendMediaUserRejectionEmailUnauthorized() throws Exception {
        mockMvc.perform(post(MEDIA_REJECTION_EMAIL_URL)
                            .content(VALID_MEDIA_REJECTION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendNotificationToInactiveUsers() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(VALID_INACTIVE_USER_NOTIFICATION_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendNotificationToInactiveUsersBadRequest() throws Exception {
        mockMvc.perform(post(INACTIVE_USER_NOTIFICATION_EMAIL_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendMiReportingEmail() throws Exception {
        String miData = "field1,field2,field3";
        when(dataManagementService.getMiData()).thenReturn(miData);
        when(accountManagementService.getMiData()).thenReturn(miData);
        when(subscriptionManagementService.getAllMiData()).thenReturn(miData);
        when(subscriptionManagementService.getLocationMiData()).thenReturn(miData);

        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendMiReportingEmail() throws Exception {
        mockMvc.perform(post(MI_REPORTING_EMAIL_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendSystemAdminMissingRequesterEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingActionResult() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_RESULT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingChangeType() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_TYPE)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminMissingEmailList() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY_WITHOUT_EMAIL_LIST)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSystemAdminBadPayload() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content("content")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendSystemAdminUpdate() throws Exception {
        mockMvc.perform(post(NOTIFY_SYSTEM_ADMIN_URL)
                            .content(NOTIFY_SYSTEM_ADMIN_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(IsNull.notNullValue()));
    }

    @Test
    void testSendDeleteLocationSubscriptionEmailBadRequest() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(INVALID_CONTENT)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedSendDeleteLocationSubscriptionEmail() throws Exception {
        mockMvc.perform(post(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_URL)
                            .content(NOTIFY_LOCATION_SUBSCRIPTION_DELETE_EMAIL_BODY)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
