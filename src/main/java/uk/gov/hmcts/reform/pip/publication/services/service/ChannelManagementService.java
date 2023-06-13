package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class ChannelManagementService {

    private static final String SERVICE = "Channel Management";
    private static final String SYSTEM_HEADER = "x-system";
    private static final String FILE_TYPE_HEADER = "x-file-type";
    private static final String TRUE = "true";
    private static final int MAX_FILE_SIZE = 2_000_000;

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
                .retrieve().bodyToMono(String.class)
                .block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    /**
     * Get the stored file (PDF/Excel) for an artefact by the artefact Id.
     * @param artefactId The artefact Id of the stored file to get.
     * @param fileType The type of file (PDF/Excel).
     * @return The byte array of teh stored file.
     */
    public String getArtefactFile(UUID artefactId, FileType fileType) {
        try {
            return webClient.get()
                .uri(String.format("%s/publication/v2/%s?maxFileSize=%s", url, artefactId, MAX_FILE_SIZE))
                .header(SYSTEM_HEADER, TRUE)
                .header(FILE_TYPE_HEADER, fileType.toString())
                .attributes(clientRegistrationId("channelManagementApi"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (WebClientResponseException ex) {
            if (NOT_FOUND.equals(ex.getStatusCode())
                || PAYLOAD_TOO_LARGE.equals(ex.getStatusCode())) {
                return "";
            }
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}

