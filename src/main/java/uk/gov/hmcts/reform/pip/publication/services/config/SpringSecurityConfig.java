package uk.gov.hmcts.reform.pip.publication.services.config;

import com.azure.spring.cloud.autoconfigure.aad.AadResourceServerWebSecurityConfigurerAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("!dev")
@SuppressWarnings("java:S4502")
public class SpringSecurityConfig extends AadResourceServerWebSecurityConfigurerAdapter {

    /**
     * Add configuration logic as needed.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        super.configure(http);
    }
}
