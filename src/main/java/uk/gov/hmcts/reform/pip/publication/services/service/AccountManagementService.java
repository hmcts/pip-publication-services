package uk.gov.hmcts.reform.pip.publication.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.ServiceToServiceException;

import java.util.List;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class AccountManagementService {
    private static final String SERVICE = "Account Management";

    @Value("${service-to-service.account-management}")
    private String url;

    private final WebClient webClient;

    @Autowired
    public AccountManagementService(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<AccountMiData> getMiData() {
        try {
            return webClient.get()
                .uri(String.format("%s/account/v2/mi-data", url))
                .attributes(clientRegistrationId("accountManagementApi"))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AccountMiData>>() {})
                .block();
        } catch (WebClientResponseException ex) {
            throw new ServiceToServiceException(SERVICE, ex.getMessage());
        }
    }
}
