package uk.gov.hmcts.reform.pip.publication.services.service.filegeneration.converters;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

public interface Converter {

    /**
     * Interface method that captures the conversion of an artefact to a Html File.
     *
     * @return The converted HTML as a string;
     */
    String convert(JsonNode artefact, Map<String, String> metadata, Map<String, Object> language) throws IOException;

    /**
     * Interface method that captures the conversion of an artefact to an Excel spreadsheet.
     *
     * @return The converted Excel spreadsheet as a byte array.
     */
    default byte[] convertToExcel(JsonNode artefact) throws IOException {
        return new byte[0];
    }
}
