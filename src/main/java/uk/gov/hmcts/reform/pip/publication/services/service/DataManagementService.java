package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;

import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.ImmutableField"})
public class DataManagementService {

    private static final String SERVICE = "Data Management";
    private static final String VERIFICATION_HEADER = "verification";
    private static final String TRUE = "true";

    @Value("${service-to-service.data-management}")
    private String url;

    @Autowired
    private WebClient webClient;

    public Artefact getArtefact(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .header("x-admin", TRUE)
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
                .header("x-admin", TRUE)
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
                .header(VERIFICATION_HEADER, TRUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}
