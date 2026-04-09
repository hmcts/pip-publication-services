package uk.gov.hmcts.reform.pip.publication.services.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AzureSecretClientConfig {
    @Value("${spring.cloud.azure.active-directory.profile.tenant-id}")
    private String tenantId;

    @Value("${azure.managed-identity.client-id}")
    private String managedIdentityClientId;

    @Value("${third-party.key-vault-name}")
    private String keyVaultName;

    @Bean
    @Profile("!dev")
    public SecretClient azureSecretClient() {
        DefaultAzureCredential defaultCredential = new DefaultAzureCredentialBuilder()
            .tenantId(tenantId)
            .managedIdentityClientId(managedIdentityClientId)
            .build();

        String keyVaultUrl = "https://" + keyVaultName + ".vault.azure.net/";
        return new SecretClientBuilder()
            .credential(defaultCredential)
            .vaultUrl(keyVaultUrl)
            .buildClient();
    }

    @Bean
    @Profile("dev")
    public SecretClient azureSecretClientDev() {
        String keyVaultUrl = "https://" + keyVaultName + ".vault.azure.net/";
        return new SecretClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .vaultUrl(keyVaultUrl)
            .buildClient();
    }
}
