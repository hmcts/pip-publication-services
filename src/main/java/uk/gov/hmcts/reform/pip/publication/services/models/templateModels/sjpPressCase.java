package uk.gov.hmcts.reform.pip.publication.services.models.templateModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class sjpPressCase {
    String name;
    String dateOfBirth;
    String reference;
    List<String> address;
    String prosecutor;
    List<Map<String, String>> offences;
}
