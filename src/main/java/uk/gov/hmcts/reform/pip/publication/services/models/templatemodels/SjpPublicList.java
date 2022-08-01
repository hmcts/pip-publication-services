package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SjpPublicList {
    private String name;
    private String postcode;
    private String offence;
    private String prosecutor;
}