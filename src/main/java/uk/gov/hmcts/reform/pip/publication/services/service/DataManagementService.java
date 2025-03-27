package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.model.location.Location;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.FileType;
import uk.gov.hmcts.reform.pip.model.report.PublicationMiData;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.ImmutableField"})
public class DataManagementService {

    private static final String SERVICE = "Data Management";
    private static final String ADMIN_HEADER = "x-admin";
    private static final String SYSTEM_HEADER = "x-system";
    private static final String ADDITIONAL_PDF_HEADER = "x-additional-pdf";
    private static final String TRUE = "true";
    private static final int MAX_FILE_SIZE = 2_000_000;

    @Value("${service-to-service.data-management}")
    private String url;

    private final WebClient webClient;
    private final WebClient miWebClient;

    @Autowired
    public DataManagementService(WebClient webClient, WebClient miWebClient) {
        this.webClient = webClient;
        this.miWebClient = miWebClient;
    }

    public Artefact getArtefact(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .header(ADMIN_HEADER, TRUE)
                .retrieve()
                .bodyToMono(Artefact.class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public Location getLocation(String locationId) {
        try {
            return webClient.get().uri(String.format("%s/locations/%s", url, locationId))
                .retrieve()
                .bodyToMono(Location.class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public byte[] getArtefactFlatFile(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s/file", url, artefactId))
                .header(ADMIN_HEADER, TRUE)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public String getArtefactJsonBlob(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s/payload", url, artefactId))
                .header(ADMIN_HEADER, TRUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public String getArtefactSummary(UUID artefactId) {
        try {
            String response = webClient.get().uri(String.format("%s/publication/%s/summary", url, artefactId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(String.class)
                .block();

            return response == null ? "" : response;
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }

    public String getArtefactFile(UUID artefactId, FileType fileType, boolean additionalPdf) {
        try {
            return webClient.get()
                .uri(String.format("%s/publication/%s/%s?maxFileSize=%s", url, artefactId, fileType, MAX_FILE_SIZE))
                .header(SYSTEM_HEADER, TRUE)
                .header(ADDITIONAL_PDF_HEADER, String.valueOf(additionalPdf))
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

    public List<PublicationMiData> getMiData() {
        try {
            return miWebClient.get()
                .uri(String.format("%s/publication/v2/mi-data", url))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PublicationMiData>>() {})
                .block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}
