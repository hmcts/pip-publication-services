package uk.gov.hmcts.reform.pip.publication.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.HttpAuthenticationScheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.pip.publication.services.Application;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {

        @SuppressWarnings({"PMD.LawOfDemeter"})
        HttpAuthenticationScheme authenticationScheme = HttpAuthenticationScheme
            .JWT_BEARER_BUILDER
            .name("Bearer")
            .build();

        return new Docket(DocumentationType.OAS_30)
            .useDefaultResponseMessages(false)
            .securityContexts(Arrays.asList(securityContext()))
            .securitySchemes(Arrays.asList(authenticationScheme))
            .select()
            .apis(RequestHandlerSelectors.basePackage(Application.class.getPackage().getName() + ".controllers"))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("P&I Publication Service")
            .description("Use this service to support dissemination of information to various consumers. "
                             + "You can find out more about how the code works at "
                             + "https://github.com/hmcts/pip-publication-services")
            .version("1.0.0")
            .contact(new Contact("P&I Team",
                                 "https://tools.hmcts.net/confluence/display/PUBH/Publication+Hub",
                                 "publicationinformation@hmcts.net"))
            .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
            .securityReferences(defaultAuth())
            .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(new SecurityReference("Bearer", authorizationScopes));
    }

}
