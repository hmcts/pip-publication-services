package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchEmailData {
    List<String> emails;
}
