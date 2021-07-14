package uk.gov.hmcts.reform.demo.errorhandling;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Exception Response class, to standardise exceptions returned from the server.
 */
@Builder
@Data
public class ExceptionResponse {

    /**
     * The error message to return.
     */
    private String message;

    /**
     * The timestamp of when the error occurred.
     */
    private LocalDateTime timestamp;

}
