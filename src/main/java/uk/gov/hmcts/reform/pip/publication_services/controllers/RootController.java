package uk.gov.hmcts.reform.pip.publication_services.controllers;

import io.swagger.annotations.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.publication_services.errorhandling.exceptions.PublicationNotFoundException;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
@Api(tags = "Publication Services root API")
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to spring-boot-template");
    }

    /**
     * Dummy endpoint, that demonstrates how the Global Exception handler can be used to capture
     * and parse exceptions into a standard format.
     * @return A ResponseEntity
     */
    @GetMapping("/publication")
    public ResponseEntity<String> getPublications() {
        throw new PublicationNotFoundException("Publication has not been found");
    }
}
