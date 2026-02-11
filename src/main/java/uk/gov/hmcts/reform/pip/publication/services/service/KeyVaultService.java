package uk.gov.hmcts.reform.pip.publication.services.service;

import com.azure.core.exception.AzureException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.AzureSecretReadException;

@Service
public class KeyVaultService {
    private final SecretClient secretClient;

    @Autowired
    public KeyVaultService(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    public String getSecretValue(String secretName) {
        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            if (secret != null) {
                return secret.getValue();
            }
            throw new AzureSecretReadException("Secret with name: " + secretName + " not found in Key Vault");
        } catch (AzureException e) {
            throw new AzureSecretReadException("Failed to retrieve secret: " + secretName);
        }
    }
}
