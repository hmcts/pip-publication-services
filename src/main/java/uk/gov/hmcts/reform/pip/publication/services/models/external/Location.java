package uk.gov.hmcts.reform.pip.publication.services.models.external;

import lombok.Data;

import java.util.List;

@Data
public class Location {

    private Integer locationId;

    private String name;

    private List<String> jurisdiction;

    private List<String> region;

}
