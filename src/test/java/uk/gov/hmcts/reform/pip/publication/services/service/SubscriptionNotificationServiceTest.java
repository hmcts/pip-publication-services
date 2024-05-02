package uk.gov.hmcts.reform.pip.publication.services.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.publication.services.models.EmailToSend;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.FlatFileSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.emaildata.RawDataSubscriptionEmailData;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionEmail;
import uk.gov.hmcts.reform.pip.publication.services.models.request.SubscriptionTypes;
import uk.gov.hmcts.reform.pip.publication.services.utils.RedisConfigurationTestBase;
import uk.gov.service.notify.SendEmailResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL;
import static uk.gov.hmcts.reform.pip.publication.services.notify.Templates.MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
class SubscriptionNotificationServiceTest extends RedisConfigurationTestBase {
    private static final String EMAIL = "test@email.com";
    private static final String FILE_CONTENT = "123";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final Integer LOCATION_ID = 1;
    private static final String LOCATION_NAME = "Location Name";
    private static final byte[] ARTEFACT_FLAT_FILE = new byte[8];
    private static final String SUCCESS_REF_ID = "successRefId";
    private static final Map<String, Object> PERSONALISATION_MAP = Map.of("email", EMAIL);

    private final Artefact artefact = new Artefact();
    private final Location location = new Location();

    private final Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();
    private final SubscriptionEmail subscriptionEmail = new SubscriptionEmail();

    @Mock
    private SendEmailResponse sendEmailResponse;

    @MockBean
    private EmailService emailService;

    @MockBean
    private DataManagementService dataManagementService;

    @MockBean
    private ChannelManagementService channelManagementService;

    @Autowired
    private SubscriptionNotificationService notificationService;

    @BeforeEach
    void setup() {
        subscriptions.put(SubscriptionTypes.LOCATION_ID, List.of("1"));

        subscriptionEmail.setEmail(EMAIL);
        subscriptionEmail.setArtefactId(ARTEFACT_ID);
        subscriptionEmail.setSubscriptions(subscriptions);

        artefact.setArtefactId(ARTEFACT_ID);

        location.setLocationId(LOCATION_ID);
        location.setName(LOCATION_NAME);

        when(sendEmailResponse.getReference()).thenReturn(Optional.of(SUCCESS_REF_ID));
        when(emailService.sendEmail(any())).thenReturn(sendEmailResponse);
    }

    @Test
    void testFlatFileSubscriptionEmailRequest() {
        artefact.setIsFlatFile(true);

        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(location);
        when(dataManagementService.getArtefactFlatFile(ARTEFACT_ID)).thenReturn(ARTEFACT_FLAT_FILE);

        EmailToSend validEmailBodyForEmailClient = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );
        when(emailService.handleEmailGeneration(any(FlatFileSubscriptionEmailData.class),
                                                eq(MEDIA_SUBSCRIPTION_FLAT_FILE_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertThat(notificationService.subscriptionEmailRequest(subscriptionEmail))
            .as("Subscription with flat file should return successful reference ID")
            .isEqualTo(SUCCESS_REF_ID);
    }

    @Test
    void testRawDataSubscriptionEmailRequest() {
        artefact.setIsFlatFile(false);
        artefact.setListType(ListType.SJP_PUBLIC_LIST);

        when(dataManagementService.getArtefact(ARTEFACT_ID)).thenReturn(artefact);
        when(dataManagementService.getLocation(LOCATION_ID.toString())).thenReturn(location);
        when(channelManagementService.getArtefactFile(ARTEFACT_ID, FileType.PDF, false)).thenReturn(FILE_CONTENT);
        when(channelManagementService.getArtefactFile(ARTEFACT_ID, FileType.EXCEL, false)).thenReturn(FILE_CONTENT);

        EmailToSend validEmailBodyForEmailClient = new EmailToSend(
            EMAIL, MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL.getTemplate(), PERSONALISATION_MAP, SUCCESS_REF_ID
        );
        when(emailService.handleEmailGeneration(any(RawDataSubscriptionEmailData.class),
                                                eq(MEDIA_SUBSCRIPTION_RAW_DATA_EMAIL)))
            .thenReturn(validEmailBodyForEmailClient);

        assertThat(notificationService.subscriptionEmailRequest(subscriptionEmail))
            .as("Subscription with raw data file should return successful reference ID")
            .isEqualTo(SUCCESS_REF_ID);
    }
}
