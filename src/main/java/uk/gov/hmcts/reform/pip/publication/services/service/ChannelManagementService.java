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
import uk.gov.hmcts.reform.pip.publication.services.models.external.FileType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class ChannelManagementService {

    private static final String SERVICE = "Channel Management";
    private static final String SYSTEM_HEADER = "x-system";
    private static final String TRUE = "true";

    @Value("${service-to-service.channel-management}")
    private String url;

    @Autowired
    private WebClient webClient;

    /**
     * Makes a get request to channel management for the artefact summary by artefactId.
     * @param artefactId The artefact Id to get the artefact summary for.
     * @return The artefact summary string.
     */
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

    /**
     * Get the stored files (PDF/Excel) for an artefact by the artefact Id.
     * @param artefactId The artefact Id to get the stored files for.
     * @return A map of FileType enum to byte array.
     */
    public Map<FileType, byte[]> getArtefactFiles(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .header(SYSTEM_HEADER, TRUE)
                .attributes(clientRegistrationId("channelManagementApi"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<FileType, byte[]>>() {
                }).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}

