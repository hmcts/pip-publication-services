package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailLimit {
    STANDARD("1"),
    HIGH("2");

    private final String prefix;
}
