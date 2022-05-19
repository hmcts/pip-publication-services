package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.NotifyException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Location;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DataManagementService {

    @Value("${service-to-service.data-management}")
    private String url;

    @Autowired
    WebClient webClient;

    public Artefact getArtefact(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .header("verification", "true")
                .header("x-admin", "true")
                .retrieve()
                .bodyToMono(Artefact.class).block();
        } catch (WebClientResponseException ex) {
            log.error("Request to data management failed due to: " + ex.getResponseBodyAsString());
            throw new NotifyException(ex.getMessage());
        }
    }

    public Location getLocation(String locationId) {
        try {
            return webClient.get().uri(String.format("%s/courts/%s", url, locationId))
                .retrieve()
                .bodyToMono(Location.class).block();
        } catch (WebClientResponseException ex) {
            log.error("Request to data management failed due to: " + ex.getResponseBodyAsString());
            throw new NotifyException(ex.getMessage());
        }
    }

    public byte[] getArtefactFlatFile(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s/file", url, artefactId))
                .header("verification", "true")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class).block();
        } catch (WebClientResponseException ex) {
            log.error("Request to data management failed due to: " + ex.getResponseBodyAsString());
            throw new NotifyException(ex.getMessage());
        }
    }
}
