package uk.gov.hmcts.reform.pip.publication.services.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.publication.services.service.ThirdPartySubscriptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ThirdPartyControllerTest {
    private static final String SUCCESS_MESSAGE = "Successfully sent new publication to third party subscribers";

    @Mock
    private ThirdPartySubscriptionService thirdPartySubscriptionService;

    @InjectMocks
    private ThirdPartyController thirdPartyController;

    @Test
    void testSendThirdPartySubscriptionReturnsOk() {
        ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription();
        when(thirdPartySubscriptionService.sendThirdPartySubscription(thirdPartySubscription))
            .thenReturn(SUCCESS_MESSAGE);

        ResponseEntity<String> responseEntity = thirdPartyController.sendThirdPartySubscription(thirdPartySubscription);

        assertThat(responseEntity.getBody())
            .as("Response message does not match")
            .isEqualTo(SUCCESS_MESSAGE);
        assertThat(responseEntity.getStatusCode())
            .as("Response status code does not match")
            .isEqualTo(HttpStatus.OK);
    }
}
