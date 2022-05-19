package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Class template for an Email that meets the requirements GovNotify needs.
 */
@Value
public class EmailToSend {

    String emailAddress;
    String template;
    Map<String, Object> personalisation;
    String referenceId;
}
