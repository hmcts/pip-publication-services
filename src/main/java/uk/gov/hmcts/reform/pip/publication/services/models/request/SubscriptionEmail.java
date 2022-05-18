package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class SubscriptionEmail {

    String email;

    Map<SubscriptionTypes, List<String>> subscriptions = new ConcurrentHashMap<>();

    UUID artefactId;

    public void applyEmptySubscriptions(Map<SubscriptionTypes, List<String>> passedSubscriptions) {
        this.subscriptions = Map.of(SubscriptionTypes.CASE_NUMBER,
                                                            passedSubscriptions.getOrDefault(SubscriptionTypes.CASE_NUMBER, List.of()),
                                                            SubscriptionTypes.CASE_URN,
                                                            passedSubscriptions.getOrDefault(SubscriptionTypes.CASE_URN, List.of()),
                                                            SubscriptionTypes.LOCATION_ID,
                                                            passedSubscriptions.getOrDefault(SubscriptionTypes.LOCATION_ID, List.of()));
    }
}
