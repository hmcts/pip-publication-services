package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.MultiPartHelper;

import java.time.Duration;
import java.util.Collections;
import java.util.function.Consumer;

import static uk.gov.hmcts.reform.pip.model.publication.FileType.PDF;

@Service
@Slf4j
public class ThirdPartyService {

    @Value("${error-handling.num-of-retries}")
    private int numOfRetries;

    @Value("${error-handling.backoff}")
    private int backoff;

    private static final String SUCCESS_MESSAGE = "Successfully sent list to %s at: %s";
    private static final String SUCCESS_DELETE_MESSAGE = "Successfully sent deleted notification to %s at: %s";
    private static final String PDF_SUCCESS_MESSAGE = "Successfully sent PDF to %s at: %s";
    private static final String COURTEL = "Courtel";
    private static final String CATH_PROVENANCE = "CATH";

    @Autowired
    private WebClient.Builder webClient;

    /**
     * Third party call for Flat File publications.
     * @param api The API to send the publication to.
     * @param payload The payload to send.
     * @param artefact The artefact to publish.
     * @param location The location to publish.
     * @return A message representing the response.
     */
    public String handleFlatFileThirdPartyCall(String api, byte[] payload, Artefact artefact, Location location) {
        MultiValueMap<String, HttpEntity<?>> multiPartValues = MultiPartHelper.createMultiPartByteArrayBody(
            Collections.singletonList(Triple.of("file", payload, artefact.getSourceArtefactId()))
        );

        webClient.build().post().uri(api)
            .headers(this.getHttpHeadersFromArtefact(artefact, location, false))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiPartValues))
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(handleRetry(api))
            .block();
        return String.format(SUCCESS_MESSAGE, COURTEL, api);
    }

    /**
     * Third party call for JSON.
     * @param api The API to send the publication to.
     * @param payload The payload to send.
     * @param artefact The artefact to publish.
     * @param location The location to publish.
     * @return A message representing the response.
     */
    public String handleJsonThirdPartyCall(String api, Object payload, Artefact artefact, Location location) {
        webClient.build().post().uri(api)
            .headers(this.getHttpHeadersFromArtefact(artefact, location, false))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(handleRetry(api))
            .block();
        return String.format(SUCCESS_MESSAGE, COURTEL, api);
    }

    /**
     * Third party call for Deletion.
     * @param api The API to send the deleted publication to.
     * @param artefact The artefact to publish.
     * @param location The location to publish.
     * @return A message representing the response.
     */
    public String handleDeleteThirdPartyCall(String api, Artefact artefact, Location location) {
        webClient.build().post().uri(api)
            .headers(this.getHttpHeadersFromArtefact(artefact, location, false))
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(handleRetry(api))
            .block();
        return String.format(SUCCESS_DELETE_MESSAGE, COURTEL, api);
    }

    /**
     * Third party call for sending PDF for JSON publications.
     * @param api The API to send the publication to.
     * @param payload The payload to send.
     * @param artefact The artefact to publish.
     * @param location The location to publish.
     * @return A message representing the response.
     */
    public String handlePdfThirdPartyCall(String api, byte[] payload, Artefact artefact, Location location) {
        MultiValueMap<String, HttpEntity<?>> multiPartValues = MultiPartHelper.createMultiPartByteArrayBody(
            Collections.singletonList(Triple.of("file", payload,
                                                artefact.getArtefactId() + PDF.getExtension()))
        );

        webClient.build().post().uri(api)
            .headers(this.getHttpHeadersFromArtefact(artefact, location, true))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiPartValues))
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(handleRetry(api))
            .block();
        return String.format(PDF_SUCCESS_MESSAGE, COURTEL, api);
    }

    private Consumer<HttpHeaders> getHttpHeadersFromArtefact(Artefact artefact, Location location,
                                                             boolean isSendingPdf) {
        if (artefact == null || location == null) {
            return httpHeaders -> { };
        }

        return httpHeaders -> {
            httpHeaders.add("x-provenance", isSendingPdf ? CATH_PROVENANCE : artefact.getProvenance());
            httpHeaders.add("x-source-artefact-id", artefact.getSourceArtefactId());
            httpHeaders.add("x-type", artefact.getType().toString());
            httpHeaders.add("x-list-type", artefact.getListType().toString());
            httpHeaders.add("x-content-date", artefact.getContentDate().toString());

            httpHeaders.add("x-sensitivity",
                artefact.getSensitivity() != null ? artefact.getSensitivity().toString() :
                    Sensitivity.PUBLIC.toString());

            httpHeaders.add("x-language", artefact.getLanguage().toString());
            httpHeaders.add("x-display-from", artefact.getDisplayFrom().toString());
            httpHeaders.add("x-display-to", artefact.getDisplayTo().toString());
            httpHeaders.add("x-location-name", location.getName());
            httpHeaders.add("x-location-jurisdiction", String.join(",", location.getJurisdiction()));
            httpHeaders.add("x-location-region", String.join(",", location.getRegion()));
        };
    }

    private Retry handleRetry(String api) {
        return Retry.backoff(numOfRetries, Duration.ofSeconds(backoff))
            .doAfterRetry(signal -> log.trace(
                "Request failed, retrying {}/" + numOfRetries,
                signal.totalRetries() + 1
            ))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                       new ThirdPartyServiceException(retrySignal.failure(), api));
    }
}
