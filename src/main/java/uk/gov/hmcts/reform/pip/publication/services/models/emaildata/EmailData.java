package uk.gov.hmcts.reform.pip.publication.services.models.emaildata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class EmailData {
    @NonNull
    private String email;
    private String referenceId = UUID.randomUUID().toString();
}
