package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.publication.services.models.external.Artefact;
import java.util.UUID;

@Slf4j
@Service
public class DataManagementService {

    @Value("${service-to-service.publication-services}")
    private String url;

    @Autowired
    WebClient webClient;

    public Artefact getArtefact(UUID artefactId) {
        try {
            return webClient.get().uri(String.format("%s/publication/%s", url, artefactId))
                .retrieve()
                .bodyToMono(Artefact.class).block();
        } catch (WebClientResponseException ex) {
            log.error("Request to data management failed due to: " + ex.getResponseBodyAsString());
        }
        return null;
    }
}
//TODO: mandory vals, need to fill out flat file version, check over raw version. tests, and chart values
