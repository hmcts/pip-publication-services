package uk.gov.hmcts.reform.pip.publication.services.models.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
@AllArgsConstructor

@Data
@Value
public class MediaRejectionEmail {
    String fullName;
    String email;
    String reasons;

}
