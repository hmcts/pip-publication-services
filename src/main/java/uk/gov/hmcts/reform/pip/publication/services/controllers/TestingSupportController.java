package uk.gov.hmcts.reform.pip.publication.services.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.publication.services.models.ThirdPartyPublicationMetadata;

import java.io.IOException;
import java.util.UUID;

@RestController
@Tag(name = "Publication Services Testing Support API")
@RequestMapping("/testing-support")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class TestingSupportController {
    @Operation(summary = "Third-party new publication test endpoint")
    @PostMapping(value = "/third-party", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> test(
        @RequestPart ThirdPartyPublicationMetadata metadata,
        @RequestPart(required = false) String payload,
        @RequestPart(required = false) MultipartFile file) throws IOException {
        return ResponseEntity.ok("New publication test");
    }

    @Operation(summary = "Third-party updated publication test endpoint")
    @PutMapping(value = "/third-party/{publicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> test(
        @PathVariable UUID publicationId,
        @RequestPart ThirdPartyPublicationMetadata metadata,
        @RequestPart(required = false) String payload,
        @RequestPart(required = false) MultipartFile file) throws IOException {
        return ResponseEntity.ok("Update publication test");
    }

    @Operation(summary = "Third-party deleted publication test endpoint")
    @DeleteMapping("/third-party/{publicationId}")
    public ResponseEntity<String> test(
        @PathVariable UUID publicationId) {
        return ResponseEntity.ok("Delete publication test");
    }
}
