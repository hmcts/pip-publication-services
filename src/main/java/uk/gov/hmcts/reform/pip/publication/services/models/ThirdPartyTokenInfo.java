package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyTokenInfo {
    private String accessToken;
    private long expiry;

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiry;
    }
}
