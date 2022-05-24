package uk.gov.hmcts.reform.pip.publication.services.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class MediaApplication {
    private UUID id;

    private String fullName;

    private String email;

    private String employer;

    private String image;

    private String imageName;

    private LocalDateTime requestDate;

    private String status;

    private LocalDateTime statusDate;
}
