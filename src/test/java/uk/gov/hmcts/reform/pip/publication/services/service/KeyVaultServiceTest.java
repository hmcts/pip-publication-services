package uk.gov.hmcts.reform.pip.publication.services.service;

import com.azure.core.exception.AzureException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.publication.services.errorhandling.exceptions.AzureSecretReadException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class KeyVaultServiceTest {
    private static final String SECRET_NAME = "secretName";
    private static final String SECRET_VALUE = "secretValue";

    @Mock
    private SecretClient secretClient;

    @InjectMocks
    private KeyVaultService keyVaultService;

    @Test
    void testGetSecretValueSuccess() {
        when(secretClient.getSecret(any())).thenReturn(new KeyVaultSecret(SECRET_NAME, SECRET_VALUE));

        assertThat(keyVaultService.getSecretValue(SECRET_NAME))
            .isEqualTo(SECRET_VALUE);
    }

    @Test
    void testGetSecretValueWithNotFoundSecret() {
        when(secretClient.getSecret(any())).thenReturn(null);

        assertThatThrownBy(() -> keyVaultService.getSecretValue(SECRET_NAME))
            .isInstanceOf(AzureSecretReadException.class)
            .hasMessageContaining("Secret with name: " + SECRET_NAME + " not found in Key Vault");
    }

    @Test
    void testGetSecretValueWithException() {
        when(secretClient.getSecret(any())).thenThrow(new AzureException());

        assertThatThrownBy(() -> keyVaultService.getSecretValue(SECRET_NAME))
            .isInstanceOf(AzureSecretReadException.class)
            .hasMessageContaining("Failed to retrieve secret: " + SECRET_NAME);
    }
}
