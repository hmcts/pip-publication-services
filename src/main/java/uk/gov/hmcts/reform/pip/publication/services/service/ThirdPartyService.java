package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Sensitivity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;

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

    public String handleThirdPartyCall(String api, Object payload,
                                       Artefact artefact, Location location) {
        webClient.build().post().uri(api)
            .headers(this.getHttpHeadersFromExchange(artefact, location))
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

    private Consumer<HttpHeaders> getHttpHeadersFromExchange(Artefact artefact,
                                                             Location location) {
        if (artefact == null || location == null) {
            return httpHeaders -> { };
        }

        return httpHeaders -> {
            httpHeaders.add("x-provenance", artefact.getProvenance());
            httpHeaders.add("x-source-artefact-id", artefact.getSourceArtefactId());
            httpHeaders.add("x-type", artefact.getType().toString());
            httpHeaders.add("x-list-type", artefact.getListType().toString());
            httpHeaders.add("x-content-date", artefact.getContentDate().toString());

            httpHeaders.add("x-sensitivity",
                artefact.getSensitivity() != null ? artefact.getSensitivity().toString() :
                    Sensitivity.PUBLIC.toString());

            httpHeaders.add("x-language", artefact.getLanguage().toString());

            httpHeaders.add("x-display-from",
                artefact.getDisplayFrom() != null ? artefact.getDisplayFrom().toString() :
                    LocalDateTime.now().toLocalDate().atStartOfDay().toString());

            httpHeaders.add("x-display-to", artefact.getDisplayTo().toString());
            httpHeaders.add("x-location-name", location.getName());
            httpHeaders.add("x-location-jurisdiction", String.join(",", location.getJurisdiction()));
            httpHeaders.add("x-location-region", String.join(",", location.getRegion()));
        };
    }
}
