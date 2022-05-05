package uk.gov.hmcts.reform.pip.publication.services.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwaggerConfigurationTest {

    @Test
    @DisplayName("Test that a swagger docket is created, and contains the correct documentation type")
    void testDocketCreation() {
        SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();

        Docket docket = swaggerConfiguration.api();
        assertNotNull(docket, "Docker has been created");

        DocumentationType documentationType = docket.getDocumentationType();
        assertEquals(DocumentationType.OAS_30, documentationType, "Documentation type is OK");
    }
}
