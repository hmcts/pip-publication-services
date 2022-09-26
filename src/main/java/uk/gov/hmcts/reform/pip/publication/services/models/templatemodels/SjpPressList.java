package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SjpPressList {
    String name;
    String dateOfBirth;
    String age;
    String reference1;
    List<String> referenceRemainder;
    String addressLine1;
    List<String> addressRemainder;
    String prosecutor;
    List<Map<String, String>> offences;
    Integer numberOfOffences;
}
