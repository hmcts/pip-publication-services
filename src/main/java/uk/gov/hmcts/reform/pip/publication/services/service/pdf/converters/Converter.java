package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;

public interface Converter {

    /**
     * Interface method that captures the conversion of an artefact to a Html File
     * @return The converted HTML as a string;
     */
     String convert(JsonNode artefact);

}
