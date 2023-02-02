package uk.gov.hmcts.reform.pip.publication.services.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Model that represents a media application.
 */
@Data
@AllArgsConstructor
public class MediaApplication {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private UUID id;

    private String fullName;

    private String email;

    private String employer;

    private String image;

    private String imageName;

    private LocalDateTime requestDate;

    private String status;

    private LocalDateTime statusDate;

    /**
     * Create an array of strings which will be added to the csv.
     *
     * @return An array of strings
     */
    public String[] toCsvStringArray() {
        return new String[] {
            this.fullName,
            this.email,
            this.employer,
            this.requestDate.format(DATE_TIME_FORMATTER),
            this.status,
            this.statusDate.format(DATE_TIME_FORMATTER)
        };
    }
}
