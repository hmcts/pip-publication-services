package uk.gov.hmcts.reform.pip.publication.services.models.templatemodels.SscsDailyList;

import lombok.Data;

import java.util.List;

@Data
public class Sitting
{
    String sittingStart;
    List<Hearing> listOfHearings;
    String channel;
    String judiciary;
}
