package uk.gov.hmcts.reform.pip.publication.services.service.pdf.converters;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class SjpPressListConverter implements Converter {

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public String convert(JsonNode artefact, Map<String, String> artefactValues) {
        return "";
    }
}
