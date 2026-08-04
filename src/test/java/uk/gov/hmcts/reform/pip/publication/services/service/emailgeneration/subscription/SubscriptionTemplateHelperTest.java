package uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.publication.ListType;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.SubscriptionTemplateHelper.IS_MAGISTRATES_MEDIA_PROTOCOL;
import static uk.gov.hmcts.reform.pip.publication.services.service.emailgeneration.subscription.SubscriptionTemplateHelper.IS_NOT_MAGISTRATES_MEDIA_PROTOCOL;

class SubscriptionTemplateHelperTest {

    @Test
    void shouldReturnTrueWhenListTypeIsMagistrates() {
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(ListType.MAGISTRATES_PUBLIC_LIST)).isTrue();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(
            ListType.MAGISTRATES_STANDARD_LIST)).isTrue();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(
            ListType.MAGISTRATES_ADULT_COURT_LIST_DAILY)).isTrue();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(
            ListType.MAGISTRATES_ADULT_COURT_LIST_FUTURE)).isTrue();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(
            ListType.MAGISTRATES_PUBLIC_ADULT_COURT_LIST_DAILY)).isTrue();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(
            ListType.MAGISTRATES_PUBLIC_ADULT_COURT_LIST_FUTURE)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenListTypeIsNotMagistratesOrNull() {
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(ListType.SJP_PUBLIC_LIST)).isFalse();
        assertThat(SubscriptionTemplateHelper.isMagistratesMediaProtocol(null)).isFalse();
    }

    @Test
    void shouldReturnInverseResultForIsNotMagistratesMediaProtocol() {
        assertThat(SubscriptionTemplateHelper.isNotMagistratesMediaProtocol(ListType.MAGISTRATES_PUBLIC_LIST)).isFalse();
        assertThat(SubscriptionTemplateHelper.isNotMagistratesMediaProtocol(ListType.SJP_PUBLIC_LIST)).isTrue();
        assertThat(SubscriptionTemplateHelper.isNotMagistratesMediaProtocol(null)).isTrue();
    }

    @Test
    void shouldExposeExpectedPersonalisationKeys() {
        assertThat(IS_MAGISTRATES_MEDIA_PROTOCOL).isEqualTo("is_magistrates_media_protocol");
        assertThat(IS_NOT_MAGISTRATES_MEDIA_PROTOCOL).isEqualTo("is_not_magistrates_media_protocol");
    }
}

