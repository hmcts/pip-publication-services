package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrimaryHealthList {
    private String hearingDate;
    private String caseName;
    private String duration;
    private String hearingType;
    private String venue;
}
