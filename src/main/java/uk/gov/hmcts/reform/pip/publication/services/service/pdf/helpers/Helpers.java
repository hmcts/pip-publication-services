package uk.gov.hmcts.reform.pip.publication.services.service.pdf.helpers;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class Helpers {
    public String formatTimestampToBst(String timestamp){
        Instant unzonedDateTime = Instant.parse(timestamp);
        ZoneId zone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTime = unzonedDateTime.atZone(zone);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
        return dtf.format(zonedDateTime);
    }
}
