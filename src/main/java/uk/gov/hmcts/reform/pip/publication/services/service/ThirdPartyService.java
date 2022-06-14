package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;

import java.time.Duration;

@Service
@Slf4j
public class ThirdPartyService {

    @Value("${error-handling.num-of-retries}")
    private int numOfRetries;

    @Value("${error-handling.backoff}")
    private int backoff;

    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s at: %s";
    private static final String COURTEL = "Courtel";

    @Autowired
    private WebClient.Builder webClient;

    public String handleCourtelCall(String api, Object payload) {
        webClient.build().post().uri(api)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(numOfRetries, Duration.ofSeconds(backoff))
                           .doAfterRetry(signal -> log.info(
                               "Request failed, retrying {}/" + numOfRetries,
                               signal.totalRetries() + 1
                           ))
                           .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                                      new ThirdPartyServiceException(retrySignal.failure(), api)))
            .block();
        return String.format(SUCCESS_MESSAGE, COURTEL, api);
    }
}
