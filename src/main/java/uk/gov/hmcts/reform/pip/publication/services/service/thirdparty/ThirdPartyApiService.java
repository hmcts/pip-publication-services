package uk.gov.hmcts.reform.pip.publication.services.service.thirdparty;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyHealthCheckException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ThirdPartyServiceException;
import uk.gov.hmcts.reform.pip.publication.services.helpers.MultiPartHelper;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyPublicationMetadata;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class ThirdPartyApiService {
    private final WebClient webClient;
    private final ThirdPartyOauthService thirdPartyOauthService;

    @Value("${error-handling.num-of-retries}")
    private int numOfRetries;

    @Value("${error-handling.backoff}")
    private int backoff;

    @Autowired
    public ThirdPartyApiService(WebClient webClientThirdParty, ThirdPartyOauthService thirdPartyOauthService) {
        this.webClient = webClientThirdParty;
        this.thirdPartyOauthService = thirdPartyOauthService;
    }

    public void sendNewPublicationToThirdParty(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration,
                                               ThirdPartyPublicationMetadata metadata, String payload,
                                               byte[] file, String filename) {
        String token = thirdPartyOauthService.getApiAccessToken(thirdPartyOauthConfiguration, false);
        Consumer<HttpHeaders> headers = httpHeaders -> {
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            httpHeaders.setBearerAuth(token);
        };
        MultiValueMap<String, HttpEntity<?>> multiPartBody = createMultiPartBody(metadata, payload, file, filename);

        try {
            webClient.post()
                .uri(thirdPartyOauthConfiguration.getDestinationUrl())
                .headers(headers)
                .body(BodyInserters.fromMultipartData(multiPartBody))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(handleRetry(thirdPartyOauthConfiguration.getDestinationUrl()))
                .block();
        } catch (WebClientResponseException | ThirdPartyServiceException ex) {
            log.error(writeLog("Failed to send new publication to third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId() + ex.getMessage()));
        }
    }

    public void sendUpdatedPublicationToThirdParty(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration,
                                                   ThirdPartyPublicationMetadata metadata, String payload,
                                                   byte[] file, String filename) {
        String token = thirdPartyOauthService.getApiAccessToken(thirdPartyOauthConfiguration, false);
        Consumer<HttpHeaders> headers = httpHeaders -> {
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            httpHeaders.setBearerAuth(token);
        };
        MultiValueMap<String, HttpEntity<?>> multiPartBody = createMultiPartBody(metadata, payload, file, filename);

        try {
            webClient.put()
                .uri(thirdPartyOauthConfiguration.getDestinationUrl() + "/" + metadata.getPublicationId())
                .headers(headers)
                .body(BodyInserters.fromMultipartData(multiPartBody))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(handleRetry(thirdPartyOauthConfiguration.getDestinationUrl()))
                .block();
        } catch (WebClientResponseException | ThirdPartyServiceException ex) {
            log.error(writeLog("Failed to send updated publication to third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId()));
        }
    }

    public void notifyThirdPartyOfPublicationDeletion(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration,
                                                      UUID publicationId) {
        String token = thirdPartyOauthService.getApiAccessToken(thirdPartyOauthConfiguration, false);

        try {
            webClient.delete()
                .uri(thirdPartyOauthConfiguration.getDestinationUrl() + "/" + publicationId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(handleRetry(thirdPartyOauthConfiguration.getDestinationUrl()))
                .block();
        } catch (WebClientResponseException | ThirdPartyServiceException ex) {
            log.error(writeLog("Failed to send publication deleted notification to third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId()));
        }
    }

    public void thirdPartyHealthCheck(ThirdPartyOauthConfiguration thirdPartyOauthConfiguration) {
        String token = thirdPartyOauthService.getApiAccessToken(thirdPartyOauthConfiguration, true);

        try {
            webClient.get()
                .uri(thirdPartyOauthConfiguration.getDestinationUrl())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (WebClientResponseException | ThirdPartyServiceException ex) {
            log.error(writeLog("Failed to perform health check for third party user with ID "
                                   + thirdPartyOauthConfiguration.getUserId()));
            throw new ThirdPartyHealthCheckException("Failed to send request to destination. "
                                                         + ex.getMessage());
        }
    }

    private MultiValueMap<String, HttpEntity<?>> createMultiPartBody(ThirdPartyPublicationMetadata metadata,
                                                                     String payload, byte[] file, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("metadata", metadata, MediaType.APPLICATION_JSON);

        if (StringUtils.isNotEmpty(payload)) {
            builder.part("payload", payload, MediaType.APPLICATION_JSON);
        }

        if (file != null && file.length > 0) {
            MultiPartHelper.createMultiPartByteArrayPart("file", file, filename, builder);
        }
        return builder.build();
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
