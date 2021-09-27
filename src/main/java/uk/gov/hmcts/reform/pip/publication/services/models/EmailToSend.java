package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.Value;

import java.util.Map;

@Value
public final class EmailToSend {
    String emailAddress;
    String template;
    Map<String, String> personalisation;
    String referenceId;
}
