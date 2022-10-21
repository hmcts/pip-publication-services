package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.ImmutableField"})
public class ChannelManagementService {

    private static final String SERVICE = "Channel Management";

    @Value("${service-to-service.channel-management}")
    private String url;

    @Autowired
    private WebClient webClient;

    public String getArtefactSummary(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/summary/%s", url, artefactId))
                .attributes(clientRegistrationId("channelManagementApi"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public Map<String, byte[]> getArtefactFiles(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .attributes(clientRegistrationId("channelManagementApi"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, byte[]>>() {
                }).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}

