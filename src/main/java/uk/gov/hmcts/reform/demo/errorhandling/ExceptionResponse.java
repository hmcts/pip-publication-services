package uk.gov.hmcts.reform.demo.errorhandling;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Exception Response class, to standardise exceptions returned from the server.
 */
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
