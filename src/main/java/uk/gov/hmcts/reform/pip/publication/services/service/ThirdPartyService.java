package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class ThirdPartyService {

    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s at: %s";
    private static final String COURTEL = "Courtel";

    @Autowired
    private WebClient.Builder webClient;

    public String handleCourtelCall(String api, Object payload) {
        try {
            webClient.build().post().uri(api)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class).block();
            return String.format(SUCCESS_MESSAGE, COURTEL, api);
        } catch (WebClientResponseException ex) {
            //All error handling to be done in PUB-981
            log.error(ex.getMessage());
            return "Request Failed";
        }
    }
}
