package uk.gov.hmcts.reform.pip.publication.services.configuration;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import uk.gov.hmcts.reform.pip.publication.services.config.SpringSecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringSecurityConfigTest {

    @Mock
    HttpSecurity httpSecurity;

    @Test
    void testSpringSecurityConfigCreation() throws Exception {
        SpringSecurityConfig springSecurityConfig = new SpringSecurityConfig();

        springSecurityConfig.apiFilterChain(httpSecurity);

        verify(httpSecurity, times(1)).apply(any(AadResourceServerHttpSecurityConfigurer.class));
        verify(httpSecurity, times(1)).csrf(any());
    }

}
