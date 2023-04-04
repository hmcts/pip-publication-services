package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor

@Data
@Value
public class MediaRejectionEmail {
    String fullName;
    String email;
    Map<String, List<String>> reasons;

}
