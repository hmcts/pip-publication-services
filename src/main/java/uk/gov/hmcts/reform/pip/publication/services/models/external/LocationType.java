package uk.gov.hmcts.reform.pip.publication.services.models.external;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum LocationType {
    VENUE("venue"),
    REGION("region"),
    OWNING_HEARING_LOCATION("owning hearing location"),
    NATIONAL("national");

    public final String csvInput;

    private static final Map<String, LocationType> BY_SENTENCE_CASE = new ConcurrentHashMap<>();

    static {
        for (LocationType locationType: values()) {
            BY_SENTENCE_CASE.put(locationType.csvInput, locationType);
        }
    }

    LocationType(String csvInput) {
        this.csvInput = csvInput;
    }

    public static LocationType valueOfCsv(String csvInput) {
        return BY_SENTENCE_CASE.get(csvInput.toLowerCase(Locale.ROOT).strip());
    }
}
