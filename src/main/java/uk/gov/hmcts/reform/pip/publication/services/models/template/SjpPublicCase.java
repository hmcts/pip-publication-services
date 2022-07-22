package uk.gov.hmcts.reform.pip.publication.services.models.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SjpPublicCase {
    private String name;
    private String postcode;
    private String offence;
    private String prosecutor;
}
